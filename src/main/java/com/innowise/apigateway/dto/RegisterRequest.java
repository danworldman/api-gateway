package com.innowise.apigateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank String surname,
        @NotNull LocalDate birthDate,
        @NotBlank @Email String email,

        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String role
) {}