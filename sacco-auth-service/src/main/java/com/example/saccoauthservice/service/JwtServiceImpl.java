package com.example.saccoauthservice.service;

import com.example.saccoauthservice.entity.OtpCode;
import com.example.saccoauthservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtServiceImpl implements JwtService {

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;
    private final long setupTokenTtlSeconds;

    public JwtServiceImpl(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds,
            @Value("${jwt.setup-token-ttl-seconds:600}") long setupTokenTtlSeconds) {

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.setupTokenTtlSeconds = setupTokenTtlSeconds;

        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters for HS256");
        }
    }

    // ==========================================================
    // ACCESS TOKEN
    // ==========================================================
    @Override
    public String issueAccessToken(User user) {

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTokenTtlSeconds);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "access")
                .claim("role", user.getRole().name())
                .claim("phone", user.getPhoneNumber())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    // ==========================================================
    // SETUP TOKEN (post-OTP, pre-password or pre-PIN)
    // ==========================================================
    @Override
    public String issueSetupToken(UUID userId, OtpCode.Purpose purpose) {

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(setupTokenTtlSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "setup")
                .claim("purpose", purpose.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    // ==========================================================
    // PIN SETUP TOKEN (issued after password is set)
    // ==========================================================
    @Override
    public String issuePinSetupToken(UUID userId) {

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(setupTokenTtlSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "setup")
                .claim("purpose", "PIN_SETUP")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    // ==========================================================
    // PARSE SETUP TOKEN
    // ==========================================================
    @Override
    public UUID parseSetupToken(String token) {

        Claims claims = parseOrThrow(token);

        Object type = claims.get("type");
        if (!"setup".equals(type)) {
            throw new JwtException("Token is not a setup token");
        }

        return UUID.fromString(claims.getSubject());
    }

    // ==========================================================
    // PARSE ACCESS TOKEN
    // ==========================================================
    @Override
    public UUID parseAccessToken(String token) {

        Claims claims = parseOrThrow(token);

        Object type = claims.get("type");
        if (!"access".equals(type)) {
            throw new JwtException("Token is not an access token");
        }

        return UUID.fromString(claims.getSubject());
    }

    @Override
    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    // ==========================================================
    // INTERNAL — parse and validate
    // ==========================================================
    private Claims parseOrThrow(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            throw e;
        }
    }
}