package com.example.saccoaccountservice.kafka.events;

import com.example.saccoaccountservice.entity.OutboxEvent;
import com.example.saccoaccountservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox for unpublished events and pushes them to Kafka.
 * Runs on a fixed schedule; safe to run on multiple instances because
 * the "published_at IS NULL" query + commit is idempotent — worst case
 * two instances attempt the same row, one wins the DB update.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS_BEFORE_SKIP = 10;

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 2000L)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepo.findTop100ByPublishedAtIsNullOrderByIdAsc();
        if (pending.isEmpty()) {
            return;
        }

        log.info("Outbox: found {} pending event(s) to publish", pending.size());

        for (OutboxEvent event : pending) {
            if (event.getAttempts() != null && event.getAttempts() >= MAX_ATTEMPTS_BEFORE_SKIP) {
                log.warn("Outbox: skipping eventId={} after {} attempts (last error: {})",
                        event.getEventId(), event.getAttempts(), event.getLastError());
                continue;
            }

            try {
                kafkaTemplate
                        .send(event.getTopic(), event.getKafkaKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);

                event.markPublished();
                outboxRepo.save(event);

                log.info("Outbox: published eventId={} type={} topic={} key={}",
                        event.getEventId(), event.getEventType(),
                        event.getTopic(), event.getKafkaKey());

            } catch (Exception ex) {
                event.recordFailure(ex.getMessage());
                outboxRepo.save(event);

                log.error("Outbox: failed to publish eventId={} type={} topic={} (attempt {}): {}",
                        event.getEventId(), event.getEventType(),
                        event.getTopic(), event.getAttempts(), ex.getMessage());
            }
        }
    }
}