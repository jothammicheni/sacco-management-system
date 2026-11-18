package com.example.saccoauthservice.controller;

import com.example.saccoauthservice.dto.*;
import com.example.saccoauthservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    // OTP flow (reset flows)
    // ==========================================================

    @PostMapping("/otp/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpRequestResponseDto requestOtp(@Valid @RequestBody OtpRequestDto request) {
        authService.requestOtp(request);
        return OtpRequestResponseDto.accepted(null);
    }

    @PostMapping("/otp/verify")
    public AuthTokenResponseDto verifyOtp(@Valid @RequestBody OtpVerifyDto request) {
        return authService.verifyOtp(request);
    }

    // ==========================================================
    // Credential setup (reset flows)
    // ==========================================================

    @PostMapping("/password/set")
    public AuthTokenResponseDto setPassword(@Valid @RequestBody PasswordSetDto request) {
        return authService.setPassword(request);
    }

    @PostMapping("/pin/set")
    public AuthTokenResponseDto setPin(@Valid @RequestBody PinSetDto request) {
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