package com.example.notificationservice.repository;

import com.example.notificationservice.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    // ---------- Idempotency ----------

    boolean existsByKafkaEventId(String kafkaEventId);

    Optional<NotificationLog> findByKafkaEventId(String kafkaEventId);

    // ---------- Lookups ----------

    List<NotificationLog> findByMemberIdOrderBySentAtDesc(UUID memberId);

    Page<NotificationLog> findByMemberId(UUID memberId, Pageable pageable);

    Page<NotificationLog> findByStatus(NotificationLog.Status status, Pageable pageable);

    Page<NotificationLog> findByChannel(NotificationLog.Channel channel, Pageable pageable);

    Page<NotificationLog> findByStatusAndChannel(
            NotificationLog.Status status, NotificationLog.Channel channel, Pageable pageable);

    // ---------- Counts (for monitoring) ----------

    long countByStatus(NotificationLog.Status status);

    @Query("""
            SELECT COUNT(n) FROM NotificationLog n
            WHERE n.status = :status AND n.sentAt > :since
            """)
    long countByStatusSince(
            @Param("status") NotificationLog.Status status,
            @Param("since") LocalDateTime since);

    // ---------- Cleanup ----------

    @Modifying
    @Query("DELETE FROM NotificationLog n WHERE n.sentAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}