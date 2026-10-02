package com.example.saccoauthservice.kafka.events;

import com.example.saccoauthservice.entity.OutboxEvent;
import com.example.saccoauthservice.entity.User;
import com.example.saccoauthservice.repository.OutboxEventRepository;
import com.example.saccoevents.Topics;
import com.example.saccoevents.auth.UserActivatedEvent;
import com.example.saccoevents.auth.UserOtpRequestedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserOutboxWriter {

    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    // ----------------------------------------------------------
    // 1. OTP requested — the Notification Service sends the SMS.
    // ----------------------------------------------------------
    public void writeOtpRequested(User user,
                                  String otpCode,
                                  UserOtpRequestedEvent.OtpPurpose purpose,
                                  Instant expiresAt) {

        UserOtpRequestedEvent event = new UserOtpRequestedEvent(
                user.getId(),
                user.getPhoneNumber(),
                otpCode,
                purpose,
                expiresAt,
                Instant.now()
        );

        persist(user.getId().toString(),
                Topics.USER_OTP_REQUESTED,
                "UserOtpRequestedEvent",
                event);
    }

    // ----------------------------------------------------------
    // 2. User activated — the user completed OTP + password + PIN.
    // ----------------------------------------------------------
    public void writeUserActivated(User user) {

        UserActivatedEvent event = new UserActivatedEvent(
                user.getId(),
                user.getPhoneNumber(),
                Instant.now()
        );

        persist(user.getId().toString(),
                Topics.USER_ACTIVATED,
                "UserActivatedEvent",
                event);
    }

    // ----------------------------------------------------------
    // Internal helper — build and save one OutboxEvent row.
    // ----------------------------------------------------------
    private void persist(String aggregateId,
                         String topic,
                         String eventType,
                         Object event) {

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize " + eventType + " for user " + aggregateId, e);
        }

        OutboxEvent outbox = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType("User")
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic(topic)
                .kafkaKey(aggregateId)
                .payload(payload)
                .attempts(0)
                .build();

        outboxRepo.save(outbox);

        log.debug("Outbox: wrote {} for user {} (eventId={})",
                eventType, aggregateId, outbox.getEventId());
    }
}