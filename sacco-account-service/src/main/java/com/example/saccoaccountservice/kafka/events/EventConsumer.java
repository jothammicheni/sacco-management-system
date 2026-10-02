package com.example.saccoaccountservice.kafka.events;

import com.example.saccoaccountservice.entity.ProcessedEvent;
import com.example.saccoaccountservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for consumers with idempotency via the processed_events inbox.
 * Subclasses call shouldProcess(...) at the top of the listener; if it returns
 * false, they ack and return immediately.
 */
@RequiredArgsConstructor
@Slf4j
public abstract class EventConsumer {

    private final ProcessedEventRepository processedRepo;
    private final String consumerName;

    /**
     * @return true if this event has NOT been processed before;
     *         false if we've already seen it (safe to skip)
     */
    @Transactional
    protected boolean shouldProcess(String eventId, Acknowledgment ack) {
        if (processedRepo.existsByEventIdAndConsumer(eventId, consumerName)) {
            log.info("Event {} already processed by {}, skipping", eventId, consumerName);
            ack.acknowledge();
            return false;
        }
        return true;
    }

    @Transactional
    protected void markProcessed(String eventId) {
        processedRepo.save(ProcessedEvent.builder()
                .eventId(eventId)
                .consumer(consumerName)
                .build());
    }
}