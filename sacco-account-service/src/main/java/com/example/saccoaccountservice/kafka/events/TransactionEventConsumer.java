package com.example.saccoaccountservice.kafka.events;

import com.example.saccoaccountservice.entity.Account;
import com.example.saccoaccountservice.entity.ProcessedEvent;
import com.example.saccoaccountservice.repository.AccountRepository;
import com.example.saccoaccountservice.repository.ProcessedEventRepository;
import com.example.saccoevents.transaction.TransactionCreatedEvent;
import com.example.saccoevents.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private static final String CONSUMER_NAME = "account-service:transaction-created";

    private final AccountRepository accountRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = Topics.TRANSACTION_CREATED,
            groupId = "sacco-account-group"
    )
    @Transactional
    public void onTransactionCreated(String payload, Acknowledgment ack) {

        TransactionCreatedEvent event;
        try {
            event = objectMapper.readValue(payload, TransactionCreatedEvent.class);
        } catch (Exception e) {
            log.error("Malformed TransactionCreatedEvent payload, dropping: {}", payload, e);
            ack.acknowledge();
            return;
        }

        // ---- Idempotency ----
        String eventId = event.transactionReference();
        ProcessedEvent.ProcessedEventId id =
                new ProcessedEvent.ProcessedEventId(eventId, CONSUMER_NAME);

        if (processedEventRepository.existsById(id)) {
            log.debug("Duplicate TransactionCreatedEvent {} — skipping", eventId);
            ack.acknowledge();
            return;
        }

        applyTransaction(event);

        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .consumer(CONSUMER_NAME)
                        .processedAt(LocalDateTime.now())
                        .build()
        );

        ack.acknowledge();
    }

    @Transactional
    public void applyTransaction(TransactionCreatedEvent event) {

        Account account = accountRepository
                .findByAccountNumberForUpdate(event.accountNumber())
                .orElseThrow(() -> new IllegalStateException(
                        "Account not found: " + event.accountNumber()
                                + " (tx " + event.transactionReference() + ")"));

        if (account.getStatus() == Account.Status.FROZEN) {
            throw new IllegalStateException(
                    "Cannot apply transaction to FROZEN account "
                            + account.getAccountNumber()
                            + " (tx " + event.transactionReference() + ")");
        }

        // Signed delta for the member's own account = sum of its journal lines.
        // CREDIT → +amount, DEBIT → -amount.
        BigDecimal delta = event.journalEntries().stream()
                .filter(e -> event.accountNumber().equals(e.accountNumber()))
                .map(e -> "CREDIT".equals(e.entryType())
                        ? e.amount()
                        : e.amount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (delta.signum() == 0) {
            log.debug("No delta for account {} on tx {}",
                    event.accountNumber(), event.transactionReference());
            return;
        }

        account.setBalance(account.getBalance().add(delta));
    }
}