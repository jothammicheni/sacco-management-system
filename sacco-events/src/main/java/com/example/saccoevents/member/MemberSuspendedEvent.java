package com.example.saccoevents.member;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberSuspendedEvent(
        UUID memberId,
        String reason,
        Instant occurredAt
) {}
