package com.example.saccoevents.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by: Auth Service
 * Consumed by:  Notification Service
 * Topic:        Topics.USER_ACTIVATION_REQUESTED
 * Key:          userId
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserActivationRequestedEvent(
        UUID userId,
        String email,
        String firstName,
        String activationLink,       // full URL: https://sacco.app/verify?token=...
        String verificationCode,     // 6-digit code, typed by user
        Instant linkExpiresAt,
        Instant codeExpiresAt,
        Instant occurredAt
) {}