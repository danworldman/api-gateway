package com.innowise.apigateway.service;

import com.innowise.apigateway.dto.auth.RegisterRequest;
import com.innowise.apigateway.dto.user.UserResponse;
import reactor.core.publisher.Mono;

public interface RegistrationService {
    Mono<UserResponse> register(RegisterRequest request);
}