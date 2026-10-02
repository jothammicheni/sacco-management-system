package com.example.saccoauthservice.service;

import com.example.saccoauthservice.dto.AuthTokenResponseDto;
import com.example.saccoauthservice.dto.LoginDto;
import com.example.saccoauthservice.dto.OtpRequestDto;
import com.example.saccoauthservice.dto.OtpVerifyDto;
import com.example.saccoauthservice.dto.PasswordSetDto;
import com.example.saccoauthservice.dto.PinSetDto;
import com.example.saccoauthservice.dto.UserResponseDto;

import java.util.UUID;

public interface AuthService {


    // create user on member creation

    void createNewUserOnMemberCreation( UUID memberId,
                                                   String phone,
                                                   String email);
    // ----------------------------------------------------------
    // 1. OTP FLOW (activation, password reset, PIN reset)
    // ----------------------------------------------------------

    /**
     * Generates an OTP for the given phone number and purpose.
     * Rate-limited: max 3 per 5 min, max 5 per hour, max 10 per day.
     * Publishes user.otp.requested so the Notification Service sends SMS.
     * Always returns success — never reveals whether the phone is registered.
     */
    void requestOtp(OtpRequestDto request);

    /**
     * Verifies an OTP code.
     * On success, invalidates the OTP and returns a short-lived setup token
     * that the client uses for the next step (set password or set PIN).
     * On failure, increments the OTP attempts counter.
     */
    AuthTokenResponseDto verifyOtp(OtpVerifyDto request);

    // ----------------------------------------------------------
    // 2. CREDENTIAL SETUP (password, PIN)
    // ----------------------------------------------------------

    /**
     * Sets the user's password.
     * Requires a valid setup token issued by verifyOtp with purpose=ACTIVATION
     * or PASSWORD_RESET.
     * After this succeeds, the user still needs to set a PIN before full access.
     */
    AuthTokenResponseDto setPassword(PasswordSetDto request);

    /**
     * Sets the user's PIN.
     * Requires a valid setup token issued by verifyOtp with purpose=ACTIVATION
     * or PIN_RESET.
     * On success, the user is fully activated and receives access + refresh tokens.
     */
    AuthTokenResponseDto setPin(PinSetDto request);

    // ----------------------------------------------------------
    // 3. LOGIN
    // ----------------------------------------------------------

    /**
     * Primary login: phone + password.
     * If the user has no PIN yet, returns action=SET_PIN with a pinSetupToken.
     * Otherwise, returns access + refresh tokens.
     * Rate-limited: max 5 failed attempts before 15-min lockout.
     */
    AuthTokenResponseDto login(LoginDto request);

    // ----------------------------------------------------------
    // 4. SESSION MANAGEMENT
    // ----------------------------------------------------------

    /**
     * Exchanges a valid refresh token for a new access token.
     * Revokes the old refresh token and issues a new one (rotation).
     */
    AuthTokenResponseDto refresh(String refreshToken);

    /**
     * Revokes the refresh token. Access tokens cannot be revoked
     * (they're short-lived), but a revoked refresh token means the client
     * cannot obtain new access tokens.
     */
    void logout(String refreshToken);

    // ----------------------------------------------------------
    // 5. USER INFO
    // ----------------------------------------------------------

    /**
     * Returns the current user's profile.
     * Requires a valid access token (JWT).
     */
    UserResponseDto getCurrentUser(UUID userId);
}