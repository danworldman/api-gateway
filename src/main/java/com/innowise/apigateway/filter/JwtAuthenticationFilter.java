package com.innowise.apigateway.filter;

import com.innowise.apigateway.dto.auth.ValidateTokenRequest;
import com.innowise.apigateway.dto.auth.ValidateResponse;
import lombok.RequiredArgsConstructor;
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
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/validate",
            "/oauth2/jwks"
    );

    private final WebClient authWebClient;

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, GatewayFilterChain gatewayFilterChain) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        String requestUriPath = serverHttpRequest.getURI().getPath();

        if (PUBLIC_PATHS.contains(requestUriPath)) {
            return gatewayFilterChain.filter(serverWebExchange);
        }

        List<String> authorizationHeaders = serverHttpRequest.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return handleFilterError(serverWebExchange, HttpStatus.UNAUTHORIZED);
        }

        String authorizationHeader = authorizationHeaders.getFirst();
        if (!authorizationHeader.startsWith("Bearer ")) {
            return handleFilterError(serverWebExchange, HttpStatus.UNAUTHORIZED);
        }
        String jsonWebToken = authorizationHeader.substring(7);

        ValidateTokenRequest validateTokenRequest = new ValidateTokenRequest(jsonWebToken);

        return authWebClient.post()
                .uri("/api/v1/auth/validate")
                .bodyValue(validateTokenRequest)
                .retrieve()
                .bodyToMono(ValidateResponse.class)
                .flatMap(validateResponse -> {
                    if (validateResponse != null && validateResponse.userId() != null) {
                        ServerHttpRequest mutatedHttpRequest = serverWebExchange.getRequest().mutate()
                                .headers(httpHeaders -> {
                                    httpHeaders.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                                    httpHeaders.set("X-User-Id", String.valueOf(validateResponse.userId()));
                                    httpHeaders.set("X-User-Role", validateResponse.role());
                                })
                                .build();
                        ServerWebExchange mutatedServerWebExchange = serverWebExchange.mutate().request(mutatedHttpRequest).build();
                        return gatewayFilterChain.filter(mutatedServerWebExchange);
                    } else {
                        return handleFilterError(serverWebExchange, HttpStatus.UNAUTHORIZED);
                    }
                })
                .onErrorResume(throwable -> handleFilterError(serverWebExchange, HttpStatus.UNAUTHORIZED));
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Mono<Void> handleFilterError(ServerWebExchange serverWebExchange, HttpStatus httpStatus) {
        serverWebExchange.getResponse().setStatusCode(httpStatus);
        return serverWebExchange.getResponse().setComplete();
    }
}