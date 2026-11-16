package com.example.saccoauthservice.service;

import com.example.saccoauthservice.dto.ActivateUserDto;
import com.example.saccoauthservice.dto.ActivationContextDto;
import com.example.saccoauthservice.dto.AuthTokenResponseDto;
import com.example.saccoauthservice.dto.LoginDto;
import com.example.saccoauthservice.dto.OtpRequestDto;
import com.example.saccoauthservice.dto.OtpVerifyDto;
import com.example.saccoauthservice.dto.PasswordSetDto;
import com.example.saccoauthservice.dto.PinSetDto;
import com.example.saccoauthservice.dto.UserResponseDto;

import java.util.UUID;

public interface AuthService {

    // ---------- Consumer-triggered ----------
    void createNewUserOnMemberCreation(UUID memberId, String phone, String email);

    // ---------- Magic link flow ----------
    ActivationContextDto verifyActivationLink(String token);
    AuthTokenResponseDto activateUser(ActivateUserDto request);

    // ---------- OTP flow ----------
    void requestOtp(OtpRequestDto request);
    AuthTokenResponseDto verifyOtp(OtpVerifyDto request);

    // ---------- Credentials ----------
    AuthTokenResponseDto setPassword(PasswordSetDto request);
    AuthTokenResponseDto setPin(PinSetDto request);

    // ---------- Session ----------
    AuthTokenResponseDto login(LoginDto request);
    AuthTokenResponseDto refresh(String refreshToken);
    void logout(String refreshToken);

    // ---------- User info ----------
    UserResponseDto getCurrentUser(UUID userId);
}