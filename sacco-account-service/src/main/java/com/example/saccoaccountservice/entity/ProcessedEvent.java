package com.example.saccoaccountservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Inbox for consumer-side deduplication.
 * Records which events this service has already processed, so Kafka redelivery
 * doesn't cause double processing.
 *
 * Composite primary key (event_id, consumer) so the same event can be
 * processed by multiple consumers (e.g. a service that has multiple @KafkaListener methods).
 */
@Entity
@Table(
        name = "processed_events",
        indexes = {
                @Index(name = "idx_processed_at", columnList = "processed_at")
        }
)
@IdClass(ProcessedEvent.ProcessedEventId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false,columnDefinition = "CHAR(36)", length = 100)
    private String eventId;

    @Id
    @Column(name = "consumer", nullable = false, length = 100)
    private String consumer;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    // ---------- Composite key class ----------

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessedEventId implements Serializable {

        private String eventId;
        private String consumer;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProcessedEventId that)) return false;
            return Objects.equals(eventId, that.eventId)
                    && Objects.equals(consumer, that.consumer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, consumer);
        }
    }
}