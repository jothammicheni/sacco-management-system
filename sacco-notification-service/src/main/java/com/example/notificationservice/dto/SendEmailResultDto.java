package com.example.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Returned by the email provider wrapper after attempting to send a message.
 * Used internally to decide whether to mark a notification as SENT or FAILED.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendEmailResultDto {

    private boolean success;
    private String providerReference;   // e.g., SES messageId, SendGrid X-Message-Id
    private String errorMessage;        // populated only if success = false

    public static SendEmailResultDto success(String reference) {
        return SendEmailResultDto.builder()
                .success(true)
                .providerReference(reference)
                .build();
    }

    public static SendEmailResultDto failure(String error) {
        return SendEmailResultDto.builder()
                .success(false)
                .errorMessage(error)
                .build();
    }
}