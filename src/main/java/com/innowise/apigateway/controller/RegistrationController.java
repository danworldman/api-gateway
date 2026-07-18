package com.innowise.apigateway.controller;

import com.innowise.apigateway.dto.auth.RegisterRequest;
import com.innowise.apigateway.dto.user.UserResponse;
import com.innowise.apigateway.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    public Mono<ResponseEntity<UserResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return registrationService.register(registerRequest)
                .map(userResponse -> ResponseEntity.status(HttpStatus.CREATED).body(userResponse));
    }
}