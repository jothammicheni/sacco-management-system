package com.example.saccoaccountservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "outbox_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_outbox_event_id", columnNames = "event_id")
        },
        indexes = {
                @Index(name = "idx_outbox_unpublished", columnList = "published_at, id"),
                @Index(name = "idx_outbox_aggregate",   columnList = "aggregate_type, aggregate_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "event_id", nullable = false, columnDefinition = "CHAR(36)", updatable = false)
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "kafka_key", nullable = false, length = 100)
    private String kafkaKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    // ---------- Business methods ----------

    public void markPublished() {
        this.publishedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void recordFailure(String error) {
        this.attempts = (this.attempts == null ? 0 : this.attempts) + 1;
        this.lastError = error;
    }
}