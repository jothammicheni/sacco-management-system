package com.example.saccoauthservice.dto;

import jakarta.validation.constraints.NotBlank;
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
public class LoginDto {

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^254[17]\\d{8}$",
            message = "Phone number must be a valid Kenyan number"
    )
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    private String password;
}