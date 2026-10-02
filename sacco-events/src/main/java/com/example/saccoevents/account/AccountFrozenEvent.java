package com.example.saccoevents.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountFrozenEvent(
        Long accountId,
        UUID memberId,
        String reason,
        Instant occurredAt
) {}
