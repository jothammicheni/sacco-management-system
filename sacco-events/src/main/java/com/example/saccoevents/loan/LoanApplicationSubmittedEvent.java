package com.example.saccoevents.loan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoanApplicationSubmittedEvent(
        Long loanApplicationId,
        UUID memberId,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        int repaymentPeriodMonths,
        Instant occurredAt
) {}
