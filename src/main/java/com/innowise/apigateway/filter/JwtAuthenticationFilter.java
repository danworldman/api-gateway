package com.innowise.apigateway.filter;

import com.innowise.apigateway.dto.ValidateTokenRequest;
import com.innowise.apigateway.dto.ValidateResponse;
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

        String authHeader = authHeaders.getFirst();
        if (!authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
        String token = authHeader.substring(7);

        ValidateTokenRequest body = new ValidateTokenRequest(token);

        return authWebClient.post()
                .uri("/api/v1/auth/validate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ValidateResponse.class)
                .flatMap(claims -> {
                    if (claims != null && claims.userId() != null) {
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", String.valueOf(claims.userId()))
                                .header("X-User-Role", claims.role())
                                .build();
                        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                        return chain.filter(mutatedExchange);
                    } else {
                        return onError(exchange, HttpStatus.UNAUTHORIZED);
                    }
                })
                .onErrorResume(e -> onError(exchange, HttpStatus.UNAUTHORIZED));
    }

    @Override
    public int getOrder() {
        return -1; // Высокий приоритет, выполняется до маршрутизации Netty
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}