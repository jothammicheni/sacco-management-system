package com.example.notificationservice.kafka;

import com.example.notificationservice.dto.NotificationRequestDto;
import com.example.notificationservice.service.NotificationService;
import com.example.saccoevents.Topics;
import com.example.saccoevents.auth.UserActivatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActivatedConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.USER_ACTIVATED, groupId = "sacco-notification-group")
    @Transactional
    public void onUserActivated(String payloadJson, Acknowledgment ack) {

        UserActivatedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, UserActivatedEvent.class);
        } catch (Exception e) {
            log.error("Cannot parse UserActivatedEvent: {}", payloadJson, e);
            ack.acknowledge();
            return;
        }

        if (event.email() == null || event.email().isBlank()) {
            log.info("No email for userId={}, skipping welcome email", event.userId());
            ack.acknowledge();
            return;
        }

        String dedupKey = event.userId() + ":user.activated";

        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(event.userId())
                .channel(NotificationRequestDto.Channel.EMAIL)
                .recipientAddress(event.email())
                .messageBody(buildWelcomeMessage())
                .build();

        try {
            notificationService.send(request, dedupKey);
            ack.acknowledge();
            log.info("Sent welcome email for userId={}", event.userId());
        } catch (Exception e) {
            log.error("Failed to send welcome email for userId={}: {}", event.userId(), e.getMessage(), e);
            throw e;
        }
    }

    private String buildWelcomeMessage() {
        return "Hello,\n\n"
                + "Welcome to SACCO!\n\n"
                + "Your account is now fully active. You can:\n"
                + "  - Log in with your phone number\n"
                + "  - View your SHARES balance\n"
                + "  - Apply for loans\n"
                + "  - Make deposits via M-Pesa\n\n"
                + "If you have any questions, reply to this email.\n\n"
                + "Regards,\nSACCO Team";
    }
}