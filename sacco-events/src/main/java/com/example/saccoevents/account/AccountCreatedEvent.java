package com.example.saccoevents.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by: Account Service
 * Consumed by:  Notification Service
 * Topic:        Topics.ACCOUNT_CREATED
 * Key:          accountId
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountCreatedEvent(
        Long accountId,
        String accountNumber,
        UUID memberId,
        String accountType,
        BigDecimal openingBalance,
        Instant occurredAt
) {}
