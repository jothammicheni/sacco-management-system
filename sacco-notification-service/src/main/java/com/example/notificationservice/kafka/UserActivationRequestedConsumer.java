package com.example.notificationservice.kafka;

import com.example.notificationservice.dto.NotificationRequestDto;
import com.example.notificationservice.service.NotificationService;
import com.example.saccoevents.Topics;
import com.example.saccoevents.auth.UserActivationRequestedEvent;
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
public class UserActivationRequestedConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = Topics.USER_ACTIVATION_REQUESTED,
            groupId = "sacco-notification-group"
    )
    @Transactional
    public void onActivationRequested(String payloadJson, Acknowledgment ack) {

        UserActivationRequestedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, UserActivationRequestedEvent.class);
        } catch (Exception e) {
            log.error("Cannot parse UserActivationRequestedEvent: {}", payloadJson, e);
            ack.acknowledge();
            return;
        }

        if (event.email() == null || event.email().isBlank()) {
            log.info("No email for userId={}, skipping activation email", event.userId());
            ack.acknowledge();
            return;
        }

        String dedupKey = event.userId() + ":user.activation.requested";

        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(event.userId())
                .channel(NotificationRequestDto.Channel.EMAIL)
                .recipientAddress(event.email())
                .messageBody(buildActivationMessage(event))
                .build();

        try {
            notificationService.send(request, dedupKey);
            ack.acknowledge();
            log.info("Sent activation email for userId={}", event.userId());
        } catch (Exception e) {
            log.error("Failed to send activation email for userId={}: {}",
                    event.userId(), e.getMessage(), e);
            throw e;
        }
    }

    private String buildActivationMessage(UserActivationRequestedEvent event) {
        return String.format(
                "Hello %s,\n\n"
                        + "Welcome to SACCO! Your account has been created.\n\n"
                        + "To activate it, click the link below:\n\n"
                        + "    %s\n\n"
                        + "Then enter this verification code on the page:\n\n"
                        + "    %s\n\n"
                        + "IMPORTANT:\n"
                        + "  - The link expires in 30 minutes.\n"
                        + "  - The code is required to complete activation.\n"
                        + "  - Do not share the code with anyone.\n\n"
                        + "If you did not expect this email, ignore it.\n\n"
                        + "Regards,\nSACCO Team",
                event.firstName(),
                event.activationLink(),
                event.verificationCode());
    }
}