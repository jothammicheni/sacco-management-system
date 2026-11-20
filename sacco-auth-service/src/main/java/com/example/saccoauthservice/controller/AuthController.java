package com.example.saccoauthservice.controller;

import com.example.saccoauthservice.dto.*;
import com.example.saccoauthservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==========================================================
    // Magic link activation
    // ==========================================================

    @GetMapping("/verify")
    public ActivationContextDto verifyLink(@RequestParam String token) {
        return authService.verifyActivationLink(token);
    }

    @PostMapping("/activate")
    public AuthTokenResponseDto activate(@Valid @RequestBody ActivateUserDto request) {
        return authService.activateUser(request);
    }

    // ==========================================================
    // OTP request — one endpoint per purpose
    // ==========================================================

    @PostMapping("/otp/request/activation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpRequestResponseDto requestActivationOtp(
            @Valid @RequestBody OtpRequestDto request) {
        request.setPurpose(OtpRequestDto.Purpose.ACTIVATION);
        authService.requestOtp(request);
        return OtpRequestResponseDto.accepted(null);
    }

    @PostMapping("/otp/request/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpRequestResponseDto requestPasswordResetOtp(
            @Valid @RequestBody OtpRequestDto request) {
        request.setPurpose(OtpRequestDto.Purpose.PASSWORD_RESET);
        authService.requestOtp(request);
        return OtpRequestResponseDto.accepted(null);
    }

    @PostMapping("/otp/request/pin-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpRequestResponseDto requestPinResetOtp(
            @Valid @RequestBody OtpRequestDto request) {
        request.setPurpose(OtpRequestDto.Purpose.PIN_RESET);
        authService.requestOtp(request);
        return OtpRequestResponseDto.accepted(null);
    }

    // ==========================================================
    // OTP verify — one endpoint per purpose
    // ==========================================================

    @PostMapping("/otp/verify/activation")
    public AuthTokenResponseDto verifyActivationOtp(
            @Valid @RequestBody OtpVerifyDto request) {
        request.setPurpose(OtpVerifyDto.Purpose.ACTIVATION);
        return authService.verifyOtp(request);
    }

    @PostMapping("/otp/verify/password-reset")
    public AuthTokenResponseDto verifyPasswordResetOtp(
            @Valid @RequestBody OtpVerifyDto request) {
        request.setPurpose(OtpVerifyDto.Purpose.PASSWORD_RESET);
        return authService.verifyOtp(request);
    }

    @PostMapping("/otp/verify/pin-reset")
    public AuthTokenResponseDto verifyPinResetOtp(
            @Valid @RequestBody OtpVerifyDto request) {
        request.setPurpose(OtpVerifyDto.Purpose.PIN_RESET);
        return authService.verifyOtp(request);
    }

    // ==========================================================
    // Credential setup — these are NOT otp/verify endpoints
    // ==========================================================

    @PostMapping("/password/set")
    public AuthTokenResponseDto setPassword(
            @Valid @RequestBody PasswordSetDto request) {
        return authService.setPassword(request);
    }

    @PostMapping("/pin/set")
    public AuthTokenResponseDto setPin(
            @Valid @RequestBody PinSetDto request) {
        return authService.setPin(request);
    }

    // ==========================================================
    // Session
    // ==========================================================

    @PostMapping("/login")
    public AuthTokenResponseDto login(@Valid @RequestBody LoginDto request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthTokenResponseDto refresh(@RequestParam("token") String refreshToken) {
        return authService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestParam("token") String refreshToken) {
        authService.logout(refreshToken);
    }

    @GetMapping("/me")
    public UserResponseDto me(@RequestHeader("X-User-Id") UUID userId) {
        return authService.getCurrentUser(userId);
    }
}