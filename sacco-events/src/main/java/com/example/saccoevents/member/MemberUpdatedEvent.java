package com.example.saccoevents.member;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberUpdatedEvent(
        UUID memberId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Instant occurredAt
) {}
