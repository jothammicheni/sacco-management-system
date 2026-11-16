package com.example.notificationservice.controller;

import com.example.notificationservice.dto.NotificationLogResponseDto;
import com.example.notificationservice.dto.NotificationRequestDto;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * ⚠ TEST CONTROLLER — for local development only.
 * Remove or disable before production.
 */
@RestController
@RequestMapping("/test/notifications")
@RequiredArgsConstructor
@Slf4j
public class TestNotificationController {

    private static final String DEFAULT_RECIPIENT = "jothammurimi2001@gmail.com";

    private final NotificationService notificationService;

    // ==========================================================
    // 1. SEND A BASIC TEST EMAIL — defaults to your inbox
    // ==========================================================
    @PostMapping("/email")
    @ResponseStatus(HttpStatus.OK)
    public NotificationLogResponseDto sendTestEmail(
            @RequestParam(defaultValue = DEFAULT_RECIPIENT) String to,
            @RequestParam(required = false) String body,
            @RequestParam(required = false) String idempotencyKey) {

        String message = body != null ? body
                : "Hello Jotham,\n\n"
                + "This is a test email from the SACCO Notification Service.\n"
                + "If you are reading this, the email pipeline works.\n\n"
                + "Sent at: " + java.time.LocalDateTime.now() + "\n\n"
                + "Regards,\nSACCO Team";

        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(UUID.randomUUID())
                .channel(NotificationRequestDto.Channel.EMAIL)
                .recipientAddress(to)
                .messageBody(message)
                .build();

        log.info("TEST: Sending email to {}", to);

        return notificationService.send(request, idempotencyKey);
    }

    // ==========================================================
    // 2. WELCOME EMAIL (dummy data)
    // ==========================================================
    @PostMapping("/email/welcome")
    @ResponseStatus(HttpStatus.OK)
    public NotificationLogResponseDto sendWelcomeEmail(
            @RequestParam(defaultValue = DEFAULT_RECIPIENT) String to) {

        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .channel(NotificationRequestDto.Channel.EMAIL)
                .recipientAddress(to)
                .messageBody(
                        "Hello Jane Doe,\n\n"
                                + "Welcome to SACCO!\n\n"
                                + "Your membership number is SACCO-2026-001.\n"
                                + "Your SHARES account has been created with a balance of KES 0.00.\n\n"
                                + "You can now log in with your phone number to start saving.\n\n"
                                + "Regards,\nSACCO Team")
                .build();

        return notificationService.send(request, "welcome-dummy-" + System.currentTimeMillis());
    }

    // ==========================================================
    // 3. OTP STYLE EMAIL
    // ==========================================================
    @PostMapping("/email/otp")
    @ResponseStatus(HttpStatus.OK)
    public NotificationLogResponseDto sendOtpEmail(
            @RequestParam(defaultValue = DEFAULT_RECIPIENT) String to,
            @RequestParam(defaultValue = "847293") String code) {

        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(UUID.randomUUID())
                .channel(NotificationRequestDto.Channel.EMAIL)
                .recipientAddress(to)
                .messageBody(
                        "Your SACCO verification code is: " + code + "\n\n"
                                + "This code expires in 10 minutes.\n"
                                + "If you did not request this, ignore this email.")
                .build();

        return notificationService.send(request, "otp-test-" + code);
    }

    // ==========================================================
    // 4. IDEMPOTENCY TEST — same key every time
    // ==========================================================
    @PostMapping("/email/idempotent")
    @ResponseStatus(HttpStatus.OK)
    public NotificationLogResponseDto sendIdempotentEmail(
            @RequestParam(defaultValue = DEFAULT_RECIPIENT) String to) {

        NotificationRequestDto request = NotificationRequestDto.builder()
                .memberId(UUID.randomUUID())
                .channel(NotificationRequestDto.Channel.EMAIL)
                .recipientAddress(to)
                .messageBody("This should only arrive ONCE. If you receive two, idempotency is broken.")
                .build();

        return notificationService.send(request, "idempotency-test-001");
    }

    // ==========================================================
    // 5. PING
    // ==========================================================
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "status", "ok",
                "defaultRecipient", DEFAULT_RECIPIENT
        );
    }
}