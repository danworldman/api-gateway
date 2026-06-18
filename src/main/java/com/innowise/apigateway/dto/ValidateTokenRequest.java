package com.innowise.apigateway.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
        @NotBlank(message = "Token is required")
        String token
) {}