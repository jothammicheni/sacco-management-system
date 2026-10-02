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
    private String action;        // "SET_PIN" or null
    private String pinSetupToken; // only present when action = SET_PIN

    public static AuthTokenResponseDto fullAccess(String accessToken,
                                                  String refreshToken,
                                                  long expiresIn) {
        return AuthTokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .build();
    }

    public static AuthTokenResponseDto requirePinSetup(String pinSetupToken) {
        return AuthTokenResponseDto.builder()
                .action("SET_PIN")
                .pinSetupToken(pinSetupToken)
                .build();
    }

    public static AuthTokenResponseDto requirePasswordSetup(String passwordSetupToken) {
        return AuthTokenResponseDto.builder()
                .action("SET_PASSWORD")
                .pinSetupToken(passwordSetupToken)   // reuse field; rename if you prefer
                .build();
    }
}