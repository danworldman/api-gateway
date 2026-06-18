package com.innowise.apigateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthRegisterRequest(
        @NotNull(message = "User service ID is required")
        Long userServiceId,

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "Role is required")
        String role
) {}