package com.example.saccoauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivationContextDto {

    private boolean valid;
    private UUID userId;
    private String maskedEmail;
    private String phoneNumber;     // normalized, e.g. 254712345678
    private LocalDateTime expiresAt;
    private String reason;          // populated only when valid = false

    public static ActivationContextDto invalid(String reason) {
        return ActivationContextDto.builder()
                .valid(false)
                .reason(reason)
                .build();
    }
}