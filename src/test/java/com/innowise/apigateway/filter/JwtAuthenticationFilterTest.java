package com.innowise.apigateway.filter;

import com.innowise.apigateway.dto.auth.ValidateResponse;
import com.innowise.apigateway.testdata.GatewayTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest extends GatewayTestData {

    @Mock
    private WebClient authWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Test
    void shouldReturnUnauthorized_whenAuthHeaderIsMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/1").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = (exchange1) -> Mono.empty();

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnUnauthorized_whenAuthHeaderIsInvalid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/1")
                .header(HttpHeaders.AUTHORIZATION, "InvalidToken")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = (exchange1) -> Mono.empty();

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldForwardRequest_whenTokenValid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = (exchange1) -> Mono.empty();

        doReturn(requestBodyUriSpec).when(authWebClient).post();
        doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri("/api/v1/auth/validate");
        doReturn(requestBodyUriSpec).when(requestBodyUriSpec).bodyValue(any());
        doReturn(responseSpec).when(requestBodyUriSpec).retrieve();
        doReturn(Mono.just(new ValidateResponse(DEFAULT_USER_ID, DEFAULT_ROLE)))
                .when(responseSpec).bodyToMono(ValidateResponse.class);

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }
}