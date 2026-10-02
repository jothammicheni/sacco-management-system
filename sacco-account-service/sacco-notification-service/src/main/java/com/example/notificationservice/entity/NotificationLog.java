package com.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "notification_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_notification_logs_kafka_event",
                        columnNames = "kafka_event_id")
        },
        indexes = {
                @Index(name = "idx_notification_logs_member_id",
                        columnList = "member_id"),
                @Index(name = "idx_notification_logs_status_sent_at",
                        columnList = "status, sent_at"),
                @Index(name = "idx_notification_logs_deleted_at",
                        columnList = "deleted_at")
        }
)
@SQLDelete(sql = "UPDATE notification_logs SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Logical reference to sacco_member_db.members.id.
     * NOT a JPA relationship — cross-service references are by id only.
     */
    @Column(name = "member_id", nullable = false, length = 36)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private Channel channel;

    @Column(name = "recipient_address", nullable = false, length = 255)
    private String recipientAddress;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status;

    /**
     * Deduplication key from the incoming Kafka event.
     * UNIQUE — prevents sending the same notification twice on redelivery.
     */
    @Column(name = "kafka_event_id", nullable = false, length = 100)
    private String kafkaEventId;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ---------- Enums ----------

    public enum Channel {
        SMS,
        EMAIL
    }

    public enum Status {
        SENT,
        FAILED
    }

    // ---------- Lifecycle hooks ----------

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = Status.SENT;
        }
    }
}