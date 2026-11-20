package com.example.saccoauthservice.service;

import com.example.saccoauthservice.dto.*;
import com.example.saccoauthservice.entity.ActivationToken;
import com.example.saccoauthservice.entity.OtpCode;
import com.example.saccoauthservice.entity.RefreshToken;
import com.example.saccoauthservice.entity.User;
import com.example.saccoauthservice.kafka.events.UserOutboxWriter;
import com.example.saccoauthservice.repository.ActivationTokenRepository;
import com.example.saccoauthservice.repository.OtpCodeRepository;
import com.example.saccoauthservice.repository.RefreshTokenRepository;
import com.example.saccoauthservice.repository.UserRepository;
import com.example.saccoauthservice.service.AuthService;
import com.example.saccoauthservice.service.JwtService;
import com.example.saccoauthservice.utils.OtpGenerator;
import com.example.saccoevents.auth.UserOtpRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository            userRepo;
    private final OtpCodeRepository         otpRepo;
    private final ActivationTokenRepository activationTokenRepo;
    private final RefreshTokenRepository    refreshTokenRepo;

    private final UserOutboxWriter          outboxWriter;
    private final JwtService                jwtService;
    private final PasswordEncoder           passwordEncoder;

    @Value("${app.activation.base-url:http://localhost:3000}")
    private String activationBaseUrl;

    // ==========================================================
    // 1. Consumer-triggered — creates user shell + activation email
    // ==========================================================
    @Override
    @Transactional
    public void createNewUserOnMemberCreation(UUID memberId,
                                              String phone,
                                              String email) {

        if (userRepo.existsById(memberId)) {
            log.info("User already exists for memberId={}, skipping", memberId);
            return;
        }

        User user = User.builder()
                .id(memberId)
                .phoneNumber(phone)
                .email(email)
                .role(User.Role.CUSTOMER)
                .status(User.Status.PENDING)
                .build();
        User saved = userRepo.save(user);

        String linkToken = UUID.randomUUID().toString().replace("-", "");
        ActivationToken activationToken = ActivationToken.builder()
                .token(linkToken)
                .userId(saved.getId())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        activationTokenRepo.save(activationToken);

        String code = OtpGenerator.generateOtp();
        OtpCode otp = OtpCode.builder()
                .userId(saved.getId())
                .codeHash(passwordEncoder.encode(code))
                .purpose(OtpCode.Purpose.ACTIVATION)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        otpRepo.save(otp);

        String activationLink = String.format("%s/verify?token=%s",
                activationBaseUrl, linkToken);

        outboxWriter.writeUserActivationRequested(
                saved,
                activationLink,
                code,
                Instant.now().plusSeconds(30 * 60),
                Instant.now().plusSeconds(10 * 60));

        log.info("Created user shell for memberId={} and queued activation email", memberId);
    }

    // ==========================================================
    // 2. Magic link validation
    // ==========================================================
    @Override
    @Transactional
    public ActivationContextDto verifyActivationLink(String token) {

        ActivationToken activationToken = activationTokenRepo.findByToken(token)
                .orElse(null);

        if (activationToken == null) {
            return ActivationContextDto.invalid("LINK_NOT_FOUND");
        }

        if (!activationToken.isUsable()) {
            return ActivationContextDto.invalid("LINK_EXPIRED");
        }

        User user = userRepo.findById(activationToken.getUserId()).orElse(null);

        if (user == null) {
            return ActivationContextDto.invalid("USER_NOT_FOUND");
        }

        if (user.getStatus() != User.Status.PENDING) {
            return ActivationContextDto.invalid("ALREADY_ACTIVATED");
        }

        return ActivationContextDto.builder()
                .valid(true)
                .userId(user.getId())
                .maskedEmail(maskEmail(user.getEmail()))
                .phoneNumber(user.getPhoneNumber())
                .expiresAt(activationToken.getExpiresAt())
                .build();
    }

    // ==========================================================
    // 3. Complete activation (link + code + password + PIN)
    // ==========================================================
    @Override
    @Transactional
    public AuthTokenResponseDto activateUser(ActivateUserDto request) {

        // Verify link token
        ActivationToken activationToken = activationTokenRepo.findByToken(request.getToken())
                .orElseThrow(() -> new NoSuchElementException("Invalid or expired link"));

        if (!activationToken.isUsable()) {
            throw new IllegalStateException("Link has expired");
        }

        User user = userRepo.findById(activationToken.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (user.getStatus() != User.Status.PENDING) {
            throw new IllegalStateException("Account is already activated");
        }

        // Verify code
        OtpCode otp = otpRepo
                .findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), OtpCode.Purpose.ACTIVATION)
                .orElseThrow(() -> new NoSuchElementException("No active code"));

        if (!otp.isUsable()) {
            throw new IllegalStateException("Code has expired");
        }

        if (!passwordEncoder.matches(request.getCode(), otp.getCodeHash())) {
            otp.incrementAttempts();
            otpRepo.save(otp);
            throw new IllegalStateException("Invalid verification code");
        }

        // Set credentials and activate
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPinHash(passwordEncoder.encode(request.getPin()));
        user.setStatus(User.Status.ACTIVE);
        userRepo.save(user);

        // Invalidate link + code
        activationToken.setUsedAt(LocalDateTime.now());
        activationTokenRepo.save(activationToken);
        otp.markUsed();
        otpRepo.save(otp);

        // Publish activation event (for welcome email)
        outboxWriter.writeUserActivated(user);

        // Issue tokens
        String accessToken  = jwtService.issueAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        log.info("User {} fully activated via magic link", user.getId());

        return AuthTokenResponseDto.fullAccess(accessToken, refreshToken,
                jwtService.getAccessTokenTtlSeconds());
    }

    // ==========================================================
    // 4. OTP request (used for password/PIN reset later)
    // ==========================================================
    @Override
    @Transactional
    public void requestOtp(OtpRequestDto request) {

        User user = userRepo.findByPhoneNumber(request.getPhoneNumber()).orElse(null);
        if (user == null || user.getStatus() == User.Status.DISABLED) {
            log.info("OTP requested for unknown/disabled phone {}", request.getPhoneNumber());
            return;
        }

        OtpCode.Purpose purpose = OtpCode.Purpose.valueOf(request.getPurpose().name());

        long recent = otpRepo.countRecentForUser(
                user.getId(), purpose, LocalDateTime.now().minusMinutes(5));
        if (recent >= 3) {
            log.warn("OTP rate limit hit for user {}", user.getId());
            return;
        }

        otpRepo.invalidateAllForUserAndPurpose(user.getId(), purpose, LocalDateTime.now());

        String rawCode = OtpGenerator.generateOtp();
        OtpCode otp = OtpCode.builder()
                .userId(user.getId())
                .codeHash(passwordEncoder.encode(rawCode))
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        otpRepo.save(otp);

        outboxWriter.writeOtpRequested(user, rawCode,
                UserOtpRequestedEvent.OtpPurpose.valueOf(purpose.name()),
                OtpGenerator.validTill());
    }

    // ==========================================================
    // 5. OTP verify (used for reset flows)
    // ==========================================================
    @Override
    @Transactional
    public AuthTokenResponseDto verifyOtp(OtpVerifyDto request) {

        User user = userRepo.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new NoSuchElementException("Invalid phone or code"));

        OtpCode.Purpose purpose = OtpCode.Purpose.valueOf(request.getPurpose().name());

        OtpCode otp = otpRepo
                .findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), purpose)
                .orElseThrow(() -> new NoSuchElementException("No active OTP for this purpose"));

        if (!otp.isUsable()) {
            throw new IllegalStateException("OTP expired or too many attempts");
        }

        if (!passwordEncoder.matches(request.getCode(), otp.getCodeHash())) {
            otp.incrementAttempts();
            otpRepo.save(otp);
            throw new IllegalStateException("Invalid OTP code");
        }

        otp.markUsed();
        otpRepo.save(otp);

        String setupToken = jwtService.issueSetupToken(user.getId(), purpose);

        return switch (purpose) {
            case ACTIVATION, PASSWORD_RESET ->
                    AuthTokenResponseDto.requirePasswordSetup(setupToken);
            case PIN_RESET ->
                    AuthTokenResponseDto.requirePinSetup(setupToken);
        };
    }

    // ==========================================================
    // 6. Credential setters (used by reset flows)
    // ==========================================================
    @Override
    @Transactional
    public AuthTokenResponseDto setPassword(PasswordSetDto request) {

        UUID userId = jwtService.parseSetupToken(request.getSetupToken());
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        if (!user.hasPin()) {
            userRepo.save(user);
            return AuthTokenResponseDto.requirePinSetup(
                    jwtService.issuePinSetupToken(user.getId()));
        }

        user.setStatus(User.Status.ACTIVE);
        userRepo.save(user);
        outboxWriter.writeUserActivated(user);

        return AuthTokenResponseDto.fullAccess(
                jwtService.issueAccessToken(user),
                issueRefreshToken(user),
                jwtService.getAccessTokenTtlSeconds());
    }

    @Override
    @Transactional
    public AuthTokenResponseDto setPin(PinSetDto request) {

        UUID userId = jwtService.parseSetupToken(request.getSetupToken());
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        user.setPinHash(passwordEncoder.encode(request.getPin()));
        user.setStatus(User.Status.ACTIVE);
        userRepo.save(user);
        outboxWriter.writeUserActivated(user);

        return AuthTokenResponseDto.fullAccess(
                jwtService.issueAccessToken(user),
                issueRefreshToken(user),
                jwtService.getAccessTokenTtlSeconds());
    }

    // ==========================================================
    // 7. Login / refresh / logout
    // ==========================================================
    @Override
    @Transactional
    public AuthTokenResponseDto login(LoginDto request) {

        User user = userRepo.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new NoSuchElementException("Invalid credentials"));

        if (user.isLocked()) {
            throw new IllegalStateException("Account locked. Try again later.");
        }

        if (!user.hasPassword()) {
            throw new IllegalStateException("Password not set for this account");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            if (user.getLoginAttempts() >= 5) {
                user.setLoginLockedUntil(LocalDateTime.now().plusMinutes(15));
            }
            userRepo.save(user);
            throw new IllegalStateException("Invalid credentials");
        }

        user.setLoginAttempts(0);
        user.setLoginLockedUntil(null);

        if (!user.hasPin()) {
            userRepo.save(user);
            return AuthTokenResponseDto.requirePinSetup(
                    jwtService.issuePinSetupToken(user.getId()));
        }

        userRepo.save(user);

        return AuthTokenResponseDto.fullAccess(
                jwtService.issueAccessToken(user),
                issueRefreshToken(user),
                jwtService.getAccessTokenTtlSeconds());
    }

    @Override
    @Transactional
    public AuthTokenResponseDto refresh(String refreshToken) {

        RefreshToken token = refreshTokenRepo.findByToken(refreshToken)
                .orElseThrow(() -> new NoSuchElementException("Invalid refresh token"));

        if (!token.isUsable()) {
            throw new IllegalStateException("Refresh token expired or revoked");
        }

        User user = userRepo.findById(token.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        token.revoke();
        refreshTokenRepo.save(token);

        return AuthTokenResponseDto.fullAccess(
                jwtService.issueAccessToken(user),
                issueRefreshToken(user),
                jwtService.getAccessTokenTtlSeconds());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepo.findByToken(refreshToken).ifPresent(t -> {
            t.revoke();
            refreshTokenRepo.save(t);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return UserResponseDto.fromEntity(user);
    }

    // ==========================================================
    // Helpers
    // ==========================================================
    private String issueRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        refreshTokenRepo.save(token);
        return token.getToken();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() <= 2
                ? "*".repeat(local.length())
                : local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1);
        return masked + "@" + parts[1];
    }
}