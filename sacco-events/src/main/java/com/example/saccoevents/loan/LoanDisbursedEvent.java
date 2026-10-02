package com.example.saccoevents.loan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by: Loan Service
 * Consumed by:  Account Service, Notification Service
 * Topic:        Topics.LOAN_DISBURSED
 * Key:          loanId
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoanDisbursedEvent(
        Long loanId,
        UUID memberId,
        BigDecimal amountDisbursed,
        BigDecimal interestRate,
        int repaymentPeriodMonths,
        Instant occurredAt
) {}
