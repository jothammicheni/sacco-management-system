package com.example.saccoauthservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRequestDto {

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^254[17]\\d{8}$",
            message = "Phone number must be a valid Kenyan number, e.g. 254712345678"
    )
    private String phoneNumber;

    @NotNull(message = "Purpose is required")
    private Purpose purpose;

    public enum Purpose {
        ACTIVATION,
        PASSWORD_RESET,
        PIN_RESET
    }
}