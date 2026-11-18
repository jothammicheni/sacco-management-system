package com.example.apigateway.filter;

import com.example.apigateway.security.AccessTokenClaims;
import com.example.apigateway.security.JwtValidator;
import com.example.apigateway.security.PublicEndpoints;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID   = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    private final JwtValidator jwtValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();

        // Always strip any client-supplied identity headers — even on public paths.
        ServerWebExchange sanitized = stripIdentityHeaders(exchange);

        // Public endpoints bypass token validation (but still get sanitized).
        if (PublicEndpoints.isPublic(method, path)) {
            return chain.filter(sanitized);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.debug("Rejecting {} {} — missing bearer token", method, path);
            return unauthorized(exchange, "MISSING_TOKEN");
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            return unauthorized(exchange, "MISSING_TOKEN");
        }

        AccessTokenClaims claims;
        try {
            claims = jwtValidator.validateAccessToken(token);
        } catch (JwtException e) {
            log.debug("Rejecting {} {} — invalid token: {}", method, path, e.getMessage());
            return unauthorized(exchange, "INVALID_TOKEN");
        }

        // Inject trusted identity headers (SET, never ADD).
        ServerHttpRequest authenticated = sanitized.getRequest().mutate()
                .headers(h -> {
                    h.set(HEADER_USER_ID, claims.userId().toString());
                    h.set(HEADER_USER_ROLE, claims.role());
                })
                .build();

        return chain.filter(sanitized.mutate().request(authenticated).build());
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest sanitized = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_ID);
                    h.remove(HEADER_USER_ROLE);
                })
                .build();
        return exchange.mutate().request(sanitized).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"" + code + "\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run before routing filters so the downstream service never sees an
        // unauthenticated request and never sees a spoofed X-User-Id.
        return -100;
    }
}