package com.example.saccoevents.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountCreditedEvent(
        Long accountId,
        UUID memberId,
        BigDecimal amount,
        BigDecimal newBalance,
        String transactionReference,
        Instant occurredAt
) {}
