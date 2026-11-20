package com.example.saccoauthservice.dto;

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
public class AuthTokenResponseDto {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;       // seconds
    private String tokenType;     // "Bearer"

    /** If the user still needs to set a PIN, no access token is issued yet. */

    public  enum NextStep{
        FULL_ACCESS,
        SETUP_PASSWORD,
        SETUP_PIN
    }

    private String action;        // "SET_PIN" or null
    private String pinSetupToken; // only present when action = SET_PIN
    public  NextStep nextStep;
    public static AuthTokenResponseDto fullAccess(String accessToken,
                                                  String refreshToken,
                                                  long expiresIn) {
        return AuthTokenResponseDto.builder()
                .nextStep(NextStep.FULL_ACCESS)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .build();
    }

    public static AuthTokenResponseDto requirePinSetup(String pinSetupToken) {
        return AuthTokenResponseDto.builder()
                .action("SET_PIN")
                .nextStep(NextStep.SETUP_PIN)
                .pinSetupToken(pinSetupToken)
                .build();
    }

    public static AuthTokenResponseDto requirePasswordSetup(String passwordSetupToken) {
        return AuthTokenResponseDto.builder()
                .nextStep(NextStep.SETUP_PASSWORD)
                .action("SET_PASSWORD")
                .pinSetupToken(passwordSetupToken)   // reuse field; rename if you prefer
                .build();
    }
}