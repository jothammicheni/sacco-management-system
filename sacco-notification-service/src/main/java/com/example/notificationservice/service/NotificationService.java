package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationLogResponseDto;
import com.example.notificationservice.dto.NotificationRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    // ----------------------------------------------------------
    // 1. SEND OPERATIONS
    // ----------------------------------------------------------

    /**
     * Sends a notification through the appropriate channel (SMS or EMAIL).
     * Persists a NotificationLog row with the result (SENT or FAILED).
     * Idempotent — if a row with the same deduplication key exists, does nothing.
     *
     * Called by:
     *  - HTTP endpoint POST /notifications (admin-triggered)
     *  - Kafka consumers (event-triggered)
     *
     * @return the persisted log row, or the existing row if already sent
     */
    NotificationLogResponseDto send(NotificationRequestDto request, String deduplicationKey);

    // ----------------------------------------------------------
    // 2. READ OPERATIONS
    // ----------------------------------------------------------

    /**
     * Returns a single notification log by its database id.
     */
    NotificationLogResponseDto getById(Long id);

    /**
     * Returns a notification log by its Kafka event id (deduplication key).
     */
    NotificationLogResponseDto getByKafkaEventId(String kafkaEventId);

    /**
     * Lists all notifications for a member, newest first.
     */
    Page<NotificationLogResponseDto> getByMemberId(UUID memberId, Pageable pageable);

    /**
     * Lists notifications filtered by status, newest first.
     */
    Page<NotificationLogResponseDto> getByStatus(String status, Pageable pageable);

    /**
     * Lists notifications filtered by channel, newest first.
     */
    Page<NotificationLogResponseDto> getByChannel(String channel, Pageable pageable);

    // ----------------------------------------------------------
    // 3. MAINTENANCE
    // ----------------------------------------------------------

    /**
     * Soft-deletes a notification log (for compliance / GDPR-style erasure).
     * Most systems never call this — notification logs are usually permanent.
     */
    void softDelete(Long id);
}