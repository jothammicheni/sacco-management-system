package com.example.saccoevents.member;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by: Member Service
 * Consumed by:  Account Service, Notification Service
 * Topic:        Topics.MEMBER_CREATED
 * Key:          memberId
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberCreatedEvent(
        UUID memberId,
        String memberNumber,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Instant occurredAt
) {}
