package com.innowise.apigateway.dto.auth;

public record ValidateResponse(
        Long userId,
        String role
) {}