package com.example.saccomemberservice.kafka.events;

import com.example.saccoevents.Topics;
import com.example.saccoevents.member.MemberCreatedEvent;
import com.example.saccoevents.member.MemberDeletedEvent;
import com.example.saccoevents.member.MemberSuspendedEvent;
import com.example.saccoevents.member.MemberUpdatedEvent;
import com.example.saccomemberservice.entity.Member;
import com.example.saccomemberservice.entity.OutboxEvent;
import com.example.saccomemberservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes events to the transactional outbox.
 * Must be called from inside the caller's @Transactional method so the
 * business row and the outbox row commit or roll back together.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventWriter {

    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public void writeMemberCreated(Member member) {
        MemberCreatedEvent event = new MemberCreatedEvent(
                member.getId(),
                member.getMemberNumber(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getPhoneNumber(),
                Instant.now()
        );
        persist(member, Topics.MEMBER_CREATED, "MemberCreatedEvent", event);
    }

    public void writeMemberUpdated(Member member) {
        MemberUpdatedEvent event = new MemberUpdatedEvent(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getPhoneNumber(),
                Instant.now()
        );
        persist(member, Topics.MEMBER_UPDATED, "MemberUpdatedEvent", event);
    }

    public void writeMemberSuspended(Member member, String reason) {
        MemberSuspendedEvent event = new MemberSuspendedEvent(
                member.getId(),
                reason,
                Instant.now()
        );
        persist(member, Topics.MEMBER_SUSPENDED, "MemberSuspendedEvent", event);
    }

    public void writeMemberDeleted(Member member) {
        MemberDeletedEvent event = new MemberDeletedEvent(
                member.getId(),
                Instant.now()
        );
        persist(member, Topics.MEMBER_DELETED, "MemberDeletedEvent", event);
    }

    // ----------------------------------------------------------
    // Internal helper — builds and saves one OutboxEvent row.
    // ----------------------------------------------------------
    private void persist(Member member, String topic, String eventType, Object event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize " + eventType + " for member " + member.getId(), e);
        }

        OutboxEvent outbox = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType("Member")
                .aggregateId(member.getId().toString())
                .eventType(eventType)
                .topic(topic)
                .kafkaKey(member.getId().toString())
                .payload(payload)
                .attempts(0)
                .build();

        outboxRepo.save(outbox);

        log.debug("Outbox: wrote {} for member {} (eventId={})",
                eventType, member.getId(), outbox.getEventId());
    }
}