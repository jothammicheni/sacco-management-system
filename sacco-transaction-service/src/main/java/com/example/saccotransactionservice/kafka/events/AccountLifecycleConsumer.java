package com.example.saccotransactionservice.kafka.events;

import com.example.saccotransactionservice.entity.LedgerAccount;
import com.example.saccotransactionservice.entity.ProcessedEvent;
import com.example.saccotransactionservice.enums.AccountCategory;
import com.example.saccotransactionservice.enums.AccountStatus;
import com.example.saccotransactionservice.repository.LedgerAccountRepository;
import com.example.saccotransactionservice.repository.ProcessedEventRepository;
import com.example.saccoevents.Topics;
import com.example.saccoevents.account.AccountCreatedEvent;
import com.example.saccoevents.account.AccountFrozenEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountLifecycleConsumer {

    private static final String CONSUMER_NAME = "transaction-service:account-lifecycle";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    // ============================================================
    // ACCOUNT CREATED -> insert mirror with sourceAccountId
    // ============================================================
    @KafkaListener(topics = Topics.ACCOUNT_CREATED, groupId = "sacco-transaction-group")
    @Transactional
    public void onAccountCreated(String payload, Acknowledgment ack) {

        AccountCreatedEvent event;
        try {
            event = objectMapper.readValue(payload, AccountCreatedEvent.class);
        } catch (Exception e) {
            log.error("Malformed AccountCreatedEvent, dropping: {}", payload, e);
            ack.acknowledge();
            return;
        }

        // dedupe on accountNumber (unique natural key of the mirror)
        var id = new ProcessedEvent.ProcessedEventId(event.accountNumber(), CONSUMER_NAME);
        if (processedEventRepository.existsById(id)) {
            ack.acknowledge();
            return;
        }

        // idempotent by source id too — protects against re-publishes
        if (ledgerAccountRepository.findBySourceAccountId(event.accountId()).isPresent()) {
            log.info("Mirror already exists for sourceAccountId={}, skipping", event.accountId());
            markProcessed(id);
            ack.acknowledge();
            return;
        }

        LedgerAccount mirror = LedgerAccount.builder()
                .sourceAccountId(event.accountId())
                .accountNumber(event.accountNumber())
                .memberId(event.memberId() == null ? null : event.memberId().toString())
                .accountCategory(mapCategory(event.accountType()))
                .accountName("Member " + event.accountType() + " — " + event.accountNumber())
                .currentBalance(event.openingBalance() == null
                        ? BigDecimal.ZERO
                        : event.openingBalance())
                .status(AccountStatus.ACTIVE)
                .build();

        ledgerAccountRepository.save(mirror);
        markProcessed(id);

        log.info("Mirrored account {} (sourceId={}, category={}) with opening balance {}",
                event.accountNumber(), event.accountId(), mirror.getAccountCategory(),
                mirror.getCurrentBalance());

        ack.acknowledge();
    }

    // ============================================================
    // ACCOUNT FROZEN -> flip mirror status by sourceAccountId
    // ============================================================
    @KafkaListener(topics = Topics.ACCOUNT_FROZEN, groupId = "sacco-transaction-group")
    @Transactional
    public void onAccountFrozen(String payload, Acknowledgment ack) {

        AccountFrozenEvent event;
        try {
            event = objectMapper.readValue(payload, AccountFrozenEvent.class);
        } catch (Exception e) {
            log.error("Malformed AccountFrozenEvent, dropping: {}", payload, e);
            ack.acknowledge();
            return;
        }

        var id = new ProcessedEvent.ProcessedEventId(
                "frozen:" + event.accountId(), CONSUMER_NAME);
        if (processedEventRepository.existsById(id)) {
            ack.acknowledge();
            return;
        }

        ledgerAccountRepository.findBySourceAccountIdForUpdate(event.accountId())
                .ifPresentOrElse(
                        mirror -> {
                            mirror.setStatus(AccountStatus.FROZEN);
                            log.info("Froze mirror {} (sourceId={}) — reason: {}",
                                    mirror.getAccountNumber(), event.accountId(),
                                    event.reason());
                        },
                        () -> log.warn(
                                "AccountFrozenEvent for sourceAccountId={} but no mirror found — "
                                        + "was account.created consumed first?",
                                event.accountId()));

        markProcessed(id);
        ack.acknowledge();
    }

    // ---------- helpers ----------

    private void markProcessed(ProcessedEvent.ProcessedEventId id) {
        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(id.getEventId())
                        .consumer(CONSUMER_NAME)
                        .build()
        );
    }

    private AccountCategory mapCategory(String accountType) {
        return switch (accountType) {
            case "SHARES"          -> AccountCategory.EQUITY;
            case "DEPOSITS"        -> AccountCategory.LIABILITY;
            case "HOLIDAY_SAVINGS" -> AccountCategory.LIABILITY;
            default -> throw new IllegalArgumentException(
                    "Unknown account type: " + accountType);
        };
    }
}