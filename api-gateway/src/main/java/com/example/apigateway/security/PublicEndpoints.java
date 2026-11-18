package com.example.apigateway.security;

import org.springframework.http.HttpMethod;

import java.util.Set;

/**
 * Single source of truth for unauthenticated endpoints.
 * Add entries here when you introduce new public routes.
 */
public final class PublicEndpoints {

    private PublicEndpoints() {
    }

    private record Route(HttpMethod method, String path) {
    }

    private static final Set<Route> PUBLIC = Set.of(
            new Route(HttpMethod.POST, "/api/v1/auth/login"),
            new Route(HttpMethod.POST, "/api/v1/auth/refresh"),
            new Route(HttpMethod.POST, "/api/v1/auth/logout"),
            new Route(HttpMethod.POST, "/api/v1/auth/activate"),
            new Route(HttpMethod.GET,  "/api/v1/auth/verify"),
            new Route(HttpMethod.POST, "/api/v1/auth/otp/request"),
            new Route(HttpMethod.POST, "/api/v1/auth/otp/verify"),
            new Route(HttpMethod.POST, "/api/v1/auth/password/set"),
            new Route(HttpMethod.POST, "/api/v1/auth/pin/set")
    );

    /**
     * Matches by exact path + method. No wildcards — public endpoints must be explicit.
     * This is deliberate: accidentally exposing an authenticated endpoint is a
     * bigger risk than forgetting to whitelist a public one (which fails loudly).
     */
    public static boolean isPublic(HttpMethod method, String path) {
        return PUBLIC.contains(new Route(method, path));
    }
}