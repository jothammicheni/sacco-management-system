package com.example.apigateway.security;

import java.util.UUID;

public record AccessTokenClaims(UUID userId , String role) {
}
