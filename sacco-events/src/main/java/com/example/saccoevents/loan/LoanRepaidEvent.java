package com.example.saccoevents.loan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoanRepaidEvent(
        Long loanId,
        UUID memberId,
        BigDecimal amountPaid,
        BigDecimal remainingBalance,
        Instant occurredAt
) {}
