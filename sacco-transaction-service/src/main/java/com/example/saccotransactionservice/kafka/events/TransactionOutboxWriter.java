
package com.example.saccotransactionservice.kafka.events;

import com.example.saccoevents.Topics;
import com.example.saccoevents.transaction.TransactionCreatedEvent;
import com.example.saccotransactionservice.entity.JournalEntry;
import com.example.saccotransactionservice.entity.OutboxEvent;
import com.example.saccotransactionservice.entity.Transaction;
import com.example.saccotransactionservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionOutboxWriter {

    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    /**
     * Writes a transaction-created event to the transactional outbox.
     *
     * Must be called inside the transaction service's
     * @Transactional business method.
     */
    public void writeTransactionCreated(Transaction tx) {

        List<TransactionCreatedEvent.JournalEntryEvent> entries =
                tx.getJournalEntries()
                        .stream()
                        .map(this::toJournalEntryEvent)
                        .toList();

        TransactionCreatedEvent event = new TransactionCreatedEvent(
                tx.getTransactionReference().toString(),
                tx.getAccountNumber(),
                tx.getMemberId(),
                tx.getAmount(),
                entries,
                Instant.now()
        );

        persist(
                tx,
                Topics.TRANSACTION_CREATED,
                "TransactionCreatedEvent",
                event
        );
    }

    /**
     * Converts a JPA JournalEntry into an event DTO.
     */
    private TransactionCreatedEvent.JournalEntryEvent toJournalEntryEvent(
            JournalEntry entry) {

        return new TransactionCreatedEvent.JournalEntryEvent(
                entry.getLedgerAccount().getAccountNumber(),
                entry.getEntryType().name(),
                entry.getAmount()
        );
    }

    /**
     * Builds and saves one OutboxEvent row.
     */
    private void persist(
            Transaction tx,
            String topic,
            String eventType,
            Object event) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outbox = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .aggregateType("Transaction")
                    .aggregateId(tx.getId().toString())
                    .eventType(eventType)
                    .topic(topic)
                    .kafkaKey(tx.getId().toString())
                    .payload(payload)
                    .attempts(0)
                    .build();

            outboxRepo.save(outbox);

        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to serialize transaction event for transaction {}",
                    tx.getId(),
                    e
            );

            throw new IllegalStateException(
                    "Could not serialize transaction event",
                    e
            );
        }
    }
}
