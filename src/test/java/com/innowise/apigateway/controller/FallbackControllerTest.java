package com.innowise.apigateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void userFallback_shouldReturnServiceUnavailable() {
        Mono<ResponseEntity<ProblemDetail>> result = controller.userFallback();
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(response.getBody().getDetail()).contains("User Service");
                })
                .verifyComplete();
    }

    @Test
    void authFallback_shouldReturnServiceUnavailable() {
        Mono<ResponseEntity<ProblemDetail>> result = controller.authFallback();
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(response.getBody().getDetail()).contains("Auth Service");
                })
                .verifyComplete();
    }

    @Test
    void orderFallback_shouldReturnServiceUnavailable() {
        Mono<ResponseEntity<ProblemDetail>> result = controller.orderFallback();
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(response.getBody().getDetail()).contains("Order Service");
                })
                .verifyComplete();
    }

    @Test
    void paymentFallback_shouldReturnServiceUnavailable() {
        Mono<ResponseEntity<ProblemDetail>> result = controller.paymentFallback();
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(response.getBody().getDetail()).contains("Payment Service");
                })
                .verifyComplete();
    }
}