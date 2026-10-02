package com.example.saccoauthservice.service;

import com.example.saccoauthservice.entity.OtpCode;
import com.example.saccoauthservice.entity.User;

import java.util.UUID;

public interface JwtService {

    /** Access token — grants API access. Short-lived (15 min). */
    String issueAccessToken(User user);

    /** Setup token — proves OTP was verified. Used to set password or PIN. Short-lived (10 min). */
    String issueSetupToken(UUID userId, OtpCode.Purpose purpose);

    /** PIN setup token — issued after password is set, before PIN is set. Short-lived (10 min). */
    String issuePinSetupToken(UUID userId);

    /** Parses a setup token; returns the user id. Throws if the token is invalid or expired. */
    UUID parseSetupToken(String token);

    /** Parses an access token; returns the user id. Throws if the token is invalid or expired. */
    UUID parseAccessToken(String token);

    /** Seconds until access tokens expire. Used in the token response. */
    long getAccessTokenTtlSeconds();
}