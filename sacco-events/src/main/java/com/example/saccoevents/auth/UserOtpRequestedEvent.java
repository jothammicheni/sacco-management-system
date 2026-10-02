package com.example.saccoevents.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by: Auth Service
 * Consumed by:  Notification Service
 * Topic:        Topics.USER_OTP_REQUESTED
 * Key:          userId
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserOtpRequestedEvent(
        UUID userId,
        String phoneNumber,
        String otpCode,
        OtpPurpose purpose,
        Instant expiresAt,
        Instant occurredAt
) {
    public enum OtpPurpose {
        ACTIVATION,
        PASSWORD_RESET,
        PIN_RESET
    }
}