package com.example.saccoevents.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by: Auth Service
 * Consumed by:  Member Service, Notification Service
 * Topic:        Topics.USER_ACTIVATED
 * Key:          userId
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserActivatedEvent(
        UUID userId,
        String phoneNumber,
        Instant occurredAt
) {}