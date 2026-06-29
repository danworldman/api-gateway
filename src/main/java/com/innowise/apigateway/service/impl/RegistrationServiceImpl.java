package com.innowise.apigateway.service.impl;

import com.innowise.apigateway.dto.auth.RegisterRequest;
import com.innowise.apigateway.dto.user.UserCreateRequest;
import com.innowise.apigateway.dto.user.UserResponse;
import com.innowise.apigateway.dto.auth.AuthRegisterRequest;
import com.innowise.apigateway.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final WebClient authWebClient;
    private final WebClient userWebClient;

    @Override
    public Mono<UserResponse> register(RegisterRequest registerRequest) {
        UserCreateRequest userCreateRequest = new UserCreateRequest(
                registerRequest.name(),
                registerRequest.surname(),
                registerRequest.birthDate(),
                registerRequest.email()
        );

        return userWebClient.post()
                .uri("/api/users")
                .bodyValue(userCreateRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        clientResponse -> handleServiceError(clientResponse, "User service error: "))
                .bodyToMono(UserResponse.class)
                .flatMap(userResponse -> processAuthRegistration(registerRequest, userResponse.id())
                        .thenReturn(userResponse));
    }

    private Mono<ResponseEntity<Void>> processAuthRegistration(RegisterRequest registerRequest, Long userId) {
        AuthRegisterRequest authRegisterRequest = new AuthRegisterRequest(
                userId,
                registerRequest.username(),
                registerRequest.password(),
                registerRequest.role()
        );

        return authWebClient.post()
                .uri("/api/v1/auth/credentials")
                .bodyValue(authRegisterRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        clientResponse -> handleServiceError(clientResponse, "Auth service error: "))
                .toBodilessEntity()
                .onErrorResume(Throwable.class, throwable -> rollbackUserCreation(userId, throwable));
    }

    private Mono<Throwable> handleServiceError(ClientResponse clientResponse, String errorPrefix) {
        return clientResponse.bodyToMono(String.class)
                .map(responseBody -> new RuntimeException(errorPrefix + responseBody));
    }

    private Mono<ResponseEntity<Void>> rollbackUserCreation(Long userId, Throwable throwable) {
        return userWebClient.delete()
                .uri("/api/users/" + userId)
                .retrieve()
                .toBodilessEntity()
                .then(Mono.error(new RuntimeException("Registration rolled back. Reason: " + throwable.getMessage())));
    }
}