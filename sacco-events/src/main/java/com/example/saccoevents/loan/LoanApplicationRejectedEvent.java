package com.example.saccoevents.loan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoanApplicationRejectedEvent(
        Long loanApplicationId,
        UUID memberId,
        String reason,
        Instant occurredAt
) {}
