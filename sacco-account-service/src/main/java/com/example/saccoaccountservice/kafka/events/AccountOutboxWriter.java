package com.example.saccoaccountservice.kafka.events;

import com.example.saccoaccountservice.entity.Account;
import com.example.saccoaccountservice.entity.OutboxEvent;
import com.example.saccoaccountservice.repository.OutboxEventRepository;
import com.example.saccoevents.Topics;
import com.example.saccoevents.account.AccountCreatedEvent;
import com.example.saccoevents.account.AccountFrozenEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes account events to the transactional outbox.
 * Must be called from inside the caller's @Transactional method so the
 * business row and the outbox row commit or roll back together.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountOutboxWriter {

    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public void writeAccountCreated(Account account) {
        AccountCreatedEvent event = new AccountCreatedEvent(
                account.getId(),
                account.getAccountNumber(),
                account.getMemberId(),
                account.getAccountType().name(),
                account.getBalance(),
                Instant.now()
        );
        persist(account, Topics.ACCOUNT_CREATED, "AccountCreatedEvent", event);
    }
    public void writeAccountFrozen(Account account) {
        AccountFrozenEvent event = new AccountFrozenEvent(
                account.getId(),
                account.getMemberId(),
                "Member deleted",
                Instant.now()
        );
        persist(account, Topics.ACCOUNT_FROZEN, "AccountFrozenEvent", event);
    }


    // ----------------------------------------------------------
    // Internal helper — builds and saves one OutboxEvent row.
    // ----------------------------------------------------------
    private void persist(Account account, String topic, String eventType, Object event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize " + eventType + " for account " + account.getId(), e);
        }

        OutboxEvent outbox = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType("Account")
                .aggregateId(account.getId().toString())
                .eventType(eventType)
                .topic(topic)
                .kafkaKey(account.getId().toString())
                .payload(payload)
                .attempts(0)
                .build();

        outboxRepo.save(outbox);

        log.debug("Outbox: wrote {} for account {} (eventId={})",
                eventType, account.getId(), outbox.getEventId());
    }
}