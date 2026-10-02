package com.example.saccoaccountservice.kafka.events;

import com.example.saccoaccountservice.kafka.events.EventConsumer;
import com.example.saccoaccountservice.repository.ProcessedEventRepository;
import com.example.saccoaccountservice.service.AccountService;
import com.example.saccoevents.Topics;
import com.example.saccoevents.member.MemberCreatedEvent;
import com.example.saccoevents.member.MemberDeletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class MemberEventConsumer extends EventConsumer {
    private final AccountService accountService;
    private final ObjectMapper objectMapper;

    public MemberEventConsumer(ProcessedEventRepository processedRepo,
                               AccountService accountService,
                               ObjectMapper objectMapper) {
        super(processedRepo, "account-service");
        this.accountService = accountService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.MEMBER_CREATED, groupId = "sacco-account-group")
    @Transactional
    public void onMemberCreated(String payloadJson, Acknowledgment ack) {

        // 1. Parse the raw JSON into the event record
        MemberCreatedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, MemberCreatedEvent.class);
        } catch (Exception e) {
            log.error("Cannot parse MemberCreatedEvent from payload: {}", payloadJson, e);
            ack.acknowledge();   // skip poison message — do not retry forever
            return;
        }

        log.info("Received MemberCreatedEvent memberId={} memberNumber={}",
                event.memberId(), event.memberNumber());

        // 2. Idempotency check
        String eventId = event.memberId() + ":member.created";
        if (!shouldProcess(eventId, ack)) {
            return;
        }

        // 3. Business action
        try {
            accountService.createDefaultAccountsFor(event.memberId());
        } catch (Exception e) {
            log.error("Failed to create accounts for memberId={}: {}",
                    event.memberId(), e.getMessage(), e);
            throw e;   // do not ack — Kafka will redeliver
        }

        // 4. Mark processed and ack
        markProcessed(eventId);
        ack.acknowledge();

        log.info("Processed MemberCreatedEvent memberId={}", event.memberId());
    }


    @KafkaListener(topics = Topics.MEMBER_DELETED, groupId = "sacco-account-group")
    @Transactional
    public void onMemberdeleted(String payloadJson, Acknowledgment ack) {
     MemberDeletedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, MemberDeletedEvent.class);
        } catch (Exception e) {
            log.error("Cannot parse MemberDeletedEvent from payload: {}", payloadJson, e);
            ack.acknowledge();   // skip poison message — do not retry forever
            return;
        }

        log.info("Received MemberDeletedEvent memberId={}",
                event.memberId());

        // 2. Idempotency check
        String eventId = event.memberId() + ":member.deleted";
        if (!shouldProcess(eventId, ack)) {
            return;
        }

        // 3. Business action
        try {
            accountService.updateAcountStatusOnMemberDelete(event.memberId());
        } catch (Exception e) {
            log.error("Failed to update account status for memberId={}: {}",
                    event.memberId(), e.getMessage(), e);
            throw e;   // do not ack — Kafka will redeliver
        }

        // 4. Mark processed and ack
        markProcessed(eventId);
        ack.acknowledge();

        log.info("Processed MemberDeletedEvent memberId={}", event.memberId());

    }


}