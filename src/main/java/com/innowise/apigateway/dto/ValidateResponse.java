package com.innowise.apigateway.dto;

public record ValidateResponse(
        Long userId,
        String role
) {}