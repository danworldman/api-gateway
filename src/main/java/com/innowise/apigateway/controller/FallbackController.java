package com.innowise.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user")
    public Mono<ResponseEntity<ProblemDetail>> userFallback() {
        return createFallbackResponse("User Service");
    }

    @GetMapping("/auth")
    public Mono<ResponseEntity<ProblemDetail>> authFallback() {
        return createFallbackResponse("Auth Service");
    }

    @GetMapping("/order")
    public Mono<ResponseEntity<ProblemDetail>> orderFallback() {
        return createFallbackResponse("Order Service");
    }

    private Mono<ResponseEntity<ProblemDetail>> createFallbackResponse(String serviceName) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                serviceName + " is temporarily unavailable. Circuit Breaker tripped."
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail));
    }
}