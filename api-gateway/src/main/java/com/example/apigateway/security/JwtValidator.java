package com.example.apigateway.security;

import com.example.apigateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class JwtValidator {

    private final SecretKey signingKey;

    public JwtValidator(JwtProperties props) {
        if (props.secret() == null || props.secret().length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(
                props.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates an access token and returns its claims.
     * Throws {@link JwtException} for any invalid token (expired, wrong signature,
     * wrong type, malformed, etc.).
     */
    public AccessTokenClaims validateAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object type = claims.get("type");
        if (!"access".equals(type)) {
            throw new JwtException("Token is not an access token");
        }

        Object role = claims.get("role");
        if (role == null) {
            throw new JwtException("Token missing role claim");
        }

        try {
            UUID userId = UUID.fromString(claims.getSubject());
            return new AccessTokenClaims(userId, role.toString());
        } catch (IllegalArgumentException e) {
            throw new JwtException("Token subject is not a valid UUID");
        }
    }
}