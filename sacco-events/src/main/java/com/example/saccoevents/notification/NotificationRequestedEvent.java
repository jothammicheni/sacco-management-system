package com.example.saccoevents.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by: any service that wants to trigger a notification
 * Consumed by:  Notification Service
 * Topic:        Topics.NOTIFICATION_REQUESTED
 * Key:          memberId
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationRequestedEvent(
        UUID memberId,
        String channel,
        String recipientAddress,
        String messageBody,
        String deduplicationKey,
        Instant occurredAt
) {}
