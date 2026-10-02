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
public class OtpVerifyDto {

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^254[17]\\d{8}$",
            message = "Phone number must be a valid Kenyan number"
    )
    private String phoneNumber;

    @NotBlank(message = "Code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Code must be exactly 6 digits")
    private String code;

    @NotNull(message = "Purpose is required")
    private Purpose purpose;

    public enum Purpose {
        ACTIVATION,
        PASSWORD_RESET,
        PIN_RESET
    }
}