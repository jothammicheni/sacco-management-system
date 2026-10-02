package com.example.saccoauthservice.service;

import com.example.saccoauthservice.dto.*;
import com.example.saccoauthservice.entity.OtpCode;
import com.example.saccoauthservice.entity.RefreshToken;
import com.example.saccoauthservice.entity.User;
import com.example.saccoauthservice.kafka.events.UserOutboxWriter;
import com.example.saccoauthservice.repository.OtpCodeRepository;
import com.example.saccoauthservice.repository.RefreshTokenRepository;
import com.example.saccoauthservice.repository.UserRepository;
import com.example.saccoauthservice.service.AuthService;
import com.example.saccoauthservice.service.JwtService;
import com.example.saccoauthservice.utils.OtpGenerator;
import com.example.saccoevents.auth.UserOtpRequestedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository        userRepo;
    private final OtpCodeRepository     otpRepo;
    private final RefreshTokenRepository refreshTokenRepo;

    private final UserOutboxWriter      outboxWriter;
    private final JwtService            jwtService;
    private final PasswordEncoder       passwordEncoder;

    // ==========================================================
    // 1. Called by MemberEventConsumer when member.created arrives.
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

        String rawCode = OtpGenerator.generateOtp();

        OtpCode otp = OtpCode.builder()
                .userId(saved.getId())
                .codeHash(passwordEncoder.encode(rawCode))
                .purpose(OtpCode.Purpose.ACTIVATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        otpRepo.save(otp);

        outboxWriter.writeOtpRequested(
                saved,
                rawCode,
                UserOtpRequestedEvent.OtpPurpose.ACTIVATION,
                OtpGenerator.validTill());

        log.info("Created user shell for memberId={} phone={}", memberId, phone);
    }

    // ==========================================================
    // 2. Request an OTP (activation, password reset, PIN reset).
    // ==========================================================
    @Override
    @Transactional
    public void requestOtp(OtpRequestDto request) {

        User user = userRepo.findByPhoneNumber(request.getPhoneNumber())
                .orElse(null);

        if (user == null) {
            // Never reveal whether the phone is registered
            log.info("OTP requested for unknown phone {}", request.getPhoneNumber());
            return;
        }

        if (user.getStatus() == User.Status.DISABLED) {
            log.info("OTP request for disabled user {}", user.getId());
            return;
        }

        OtpCode.Purpose purpose = OtpCode.Purpose.valueOf(request.getPurpose().name());

        // Rate limit: max 3 in the last 5 minutes
        long recentCount = otpRepo.countRecentForUser(
                user.getId(), purpose, LocalDateTime.now().minusMinutes(5));
        if (recentCount >= 3) {
            log.warn("OTP rate limit hit for user {} purpose {}", user.getId(), purpose);
            return;   // still 202 to the client
        }

        // Invalidate previous OTPs of the same purpose
        otpRepo.invalidateAllForUserAndPurpose(user.getId(), purpose, LocalDateTime.now());

        // Generate and store the new OTP
        String rawCode = OtpGenerator.generateOtp();
        OtpCode otp = OtpCode.builder()
                .userId(user.getId())
                .codeHash(passwordEncoder.encode(rawCode))   // store HASH
                .purpose(purpose)
                .expiresAt(LocalDateTime.from(OtpGenerator.validTill()))
                .build();
        otpRepo.save(otp);

        // Publish the RAW code for the SMS
        outboxWriter.writeOtpRequested(
                user,
                rawCode,
                UserOtpRequestedEvent.OtpPurpose.valueOf(purpose.name()),
                OtpGenerator.validTill());

        log.info("OTP issued for user {} purpose {}", user.getId(), purpose);
    }

    // ==========================================================
    // 3. Verify OTP → return a setup token (SET_PASSWORD or SET_PIN).
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

        // Return a short-lived setup token based on purpose
        String setupToken = jwtService.issueSetupToken(user.getId(), purpose);

        return switch (purpose) {
            case ACTIVATION, PASSWORD_RESET ->
                    AuthTokenResponseDto.requirePasswordSetup(setupToken);
            case PIN_RESET ->
                    AuthTokenResponseDto.requirePinSetup(setupToken);
        };
    }

    // ==========================================================
    // 4. Set password using a setup token.
    // ==========================================================
    @Override
    @Transactional
    public AuthTokenResponseDto setPassword(PasswordSetDto request) {

        UUID userId = jwtService.parseSetupToken(request.getSetupToken());

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // If the user has no PIN yet, next step is PIN setup
        if (!user.hasPin()) {
            userRepo.save(user);
            String pinSetupToken = jwtService.issuePinSetupToken(user.getId());
            return AuthTokenResponseDto.requirePinSetup(pinSetupToken);
        }

        // Otherwise the user is fully activated
        user.setStatus(User.Status.ACTIVE);
        userRepo.save(user);

        outboxWriter.writeUserActivated(user);

        String accessToken  = jwtService.issueAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        return AuthTokenResponseDto.fullAccess(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    // ==========================================================
    // 5. Set PIN using a setup token. Completes activation.
    // ==========================================================
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

        String accessToken  = jwtService.issueAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        log.info("User {} fully activated", userId);

        return AuthTokenResponseDto.fullAccess(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    // ==========================================================
    // 6. Login: phone + password.
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

        // Reset counters on successful password check
        user.setLoginAttempts(0);
        user.setLoginLockedUntil(null);

        // If no PIN yet, route the user to PIN setup
        if (!user.hasPin()) {
            userRepo.save(user);
            String pinSetupToken = jwtService.issuePinSetupToken(user.getId());
            return AuthTokenResponseDto.requirePinSetup(pinSetupToken);
        }

        userRepo.save(user);

        String accessToken  = jwtService.issueAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        log.info("User {} logged in", user.getId());

        return AuthTokenResponseDto.fullAccess(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    // ==========================================================
    // 7. Refresh access token using a refresh token.
    // ==========================================================
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

        // Rotate: revoke the old token, issue a new one
        token.revoke();
        refreshTokenRepo.save(token);

        String newAccess  = jwtService.issueAccessToken(user);
        String newRefresh = issueRefreshToken(user);

        return AuthTokenResponseDto.fullAccess(newAccess, newRefresh, jwtService.getAccessTokenTtlSeconds());
    }

    // ==========================================================
    // 8. Logout: revoke a refresh token.
    // ==========================================================
    @Override
    @Transactional
    public void logout(String refreshToken) {

        refreshTokenRepo.findByToken(refreshToken).ifPresent(t -> {
            t.revoke();
            refreshTokenRepo.save(t);
        });
    }

    // ==========================================================
    // 9. Get current user.
    // ==========================================================
    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(UUID userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return UserResponseDto.fromEntity(user);
    }

    // ==========================================================
    // Internal helper — create and persist a refresh token.
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
}