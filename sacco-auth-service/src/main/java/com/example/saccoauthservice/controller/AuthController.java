package com.example.saccoauthservice.controller;

import com.example.saccoauthservice.dto.AuthTokenResponseDto;
import com.example.saccoauthservice.dto.LoginDto;
import com.example.saccoauthservice.dto.OtpRequestDto;
import com.example.saccoauthservice.dto.OtpRequestResponseDto;
import com.example.saccoauthservice.dto.OtpVerifyDto;
import com.example.saccoauthservice.dto.PasswordSetDto;
import com.example.saccoauthservice.dto.PinSetDto;
import com.example.saccoauthservice.dto.UserResponseDto;
import com.example.saccoauthservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==========================================================
    // OTP FLOW
    // ==========================================================

    @PostMapping("/otp/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpRequestResponseDto requestOtp(@Valid @RequestBody OtpRequestDto request) {
        authService.requestOtp(request);
        // Always the same response — never reveal whether the phone is registered
        return OtpRequestResponseDto.accepted(null);
    }

    @PostMapping("/otp/verify")
    public AuthTokenResponseDto verifyOtp(@Valid @RequestBody OtpVerifyDto request) {
        return authService.verifyOtp(request);
    }

    // ==========================================================
    // CREDENTIAL SETUP
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
    // LOGIN / SESSION
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

    // ==========================================================
    // CURRENT USER (requires access token)
    // ==========================================================

    @GetMapping("/me")
    public UserResponseDto me(@AuthenticationPrincipal UUID userId) {
        return authService.getCurrentUser(userId);
    }
}