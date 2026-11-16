package com.example.notificationservice.kafka;

import com.example.notificationservice.dto.NotificationRequestDto;
import com.example.notificationservice.service.NotificationService;
import com.example.saccoevents.Topics;
import com.example.saccoevents.auth.UserOtpRequestedEvent;
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
public class UserOtpRequestedConsumer {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = Topics.USER_OTP_REQUESTED,
            groupId = "sacco-notification-group"
    )
    @Transactional
    public void onOtpRequested(String payloadJson, Acknowledgment ack) {

        UserOtpRequestedEvent event;
        try {
            event = objectMapper.readValue(payloadJson, UserOtpRequestedEvent.class);
        } catch (Exception e) {
            log.error("Cannot parse UserOtpRequestedEvent: {}", payloadJson, e);
            ack.acknowledge();
            return;
        }

        log.info("Received UserOtpRequestedEvent userId={} email={} purpose={}",
                event.userId(), event.email(), event.purpose());

        if (event.email() == null || event.email().isBlank()) {
            log.info("No email for userId={}, cannot send OTP email", event.userId());
            ack.acknowledge();
            return;
        }

        String dedupKey = event.userId() + ":otp:" + event.purpose() + ":" + event.otpCode();

        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(event.userId())
                .channel(NotificationRequestDto.Channel.EMAIL)
                .recipientAddress(event.email())
                .messageBody(buildOtpMessage(event))
                .build();

        try {
            notificationService.send(request, dedupKey);
            ack.acknowledge();
            log.info("Sent OTP email for userId={} purpose={}",
                    event.userId(), event.purpose());
        } catch (Exception e) {
            log.error("Failed to send OTP email for userId={}: {}",
                    event.userId(), e.getMessage(), e);
            throw e;
        }
    }

    private String buildOtpMessage(UserOtpRequestedEvent event) {

        String action = switch (event.purpose()) {
            case ACTIVATION     -> "activate your SACCO account";
            case PASSWORD_RESET -> "reset your SACCO password";
            case PIN_RESET      -> "reset your SACCO transaction PIN";
        };

        return "Hello,\n\n"
                + "Use the code below to " + action + ":\n\n"
                + "    " + event.otpCode() + "\n\n"
                + "This code expires at " + event.expiresAt() + ".\n"
                + "If you did not request this, ignore this email.\n\n"
                + "Regards,\nSACCO Team";
    }
}