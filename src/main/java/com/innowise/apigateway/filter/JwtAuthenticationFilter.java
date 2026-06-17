package com.innowise.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final WebClient authWebClient;

    public JwtAuthenticationFilter(WebClient authWebClient) {
        this.authWebClient = authWebClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (path.contains("/auth/login") || path.contains("/register")) {
            return chain.filter(exchange);
        }

        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
        String token = authHeader.substring(7);

        ValidateTokenRequestJson body = new ValidateTokenRequestJson(token);

        return authWebClient.post()
                .uri("/api/v1/auth/validate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ValidateResponseJson.class)
                .flatMap(claims -> {
                    if (claims != null && claims.getUserId() != null) {
                        ServerHttpRequest mutatedRequest = request.mutate()
                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                .header("X-User-Id", String.valueOf(claims.getUserId()))
                                .header("X-User-Role", claims.getRole())
                                .build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    } else {
                        return onError(exchange, HttpStatus.UNAUTHORIZED);
                    }
                })
                .onErrorResume(e -> {
                    return onError(exchange, HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Data
    @AllArgsConstructor
    static class ValidateTokenRequestJson {
        private String token;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ValidateResponseJson {
        private Long userId;
        private String role;
    }
}