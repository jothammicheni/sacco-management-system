package com.example.saccoauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRequestResponseDto {

    private String message;
    private LocalDateTime expiresAt;

    public static OtpRequestResponseDto accepted(LocalDateTime expiresAt) {
        return OtpRequestResponseDto.builder()
                .message("If this phone is registered, a code has been sent.")
                .expiresAt(expiresAt)
                .build();
    }
}