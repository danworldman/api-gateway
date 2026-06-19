package com.innowise.apigateway.service;

import com.innowise.apigateway.dto.auth.RegisterRequest;
import reactor.core.publisher.Mono;

public interface RegistrationService {
    Mono<Void> register(RegisterRequest request);
}