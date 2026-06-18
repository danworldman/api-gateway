package com.innowise.apigateway.service.impl;

import com.innowise.apigateway.dto.RegisterRequest;
import com.innowise.apigateway.dto.UserCreateRequest;
import com.innowise.apigateway.dto.UserResponse;
import com.innowise.apigateway.dto.AuthRegisterRequest;
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
    public Mono<Void> register(RegisterRequest request) {
        UserCreateRequest userDto = new UserCreateRequest(
                request.name(),
                request.surname(),
                request.birthDate(),
                request.email()
        );

        return userWebClient.post()
                .uri("/api/users")
                .bodyValue(userDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        res -> handleServiceError(res, "User service error: "))
                .bodyToMono(UserResponse.class)
                .flatMap(userResponse -> processAuthRegistration(request, userResponse.id()))
                .then();
    }

    private Mono<ResponseEntity<Void>> processAuthRegistration(RegisterRequest request, Long userId) {
        AuthRegisterRequest authDto = new AuthRegisterRequest(
                userId,
                request.username(),
                request.password(),
                request.role()
        );

        return authWebClient.post()
                .uri("/api/v1/auth/register")
                .bodyValue(authDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        res -> handleServiceError(res, "Auth service error: "))
                .toBodilessEntity()
                .onErrorResume(Throwable.class, e -> rollbackUserCreation(userId, e));
    }

    private Mono<Throwable> handleServiceError(ClientResponse response, String errorPrefix) {
        return response.bodyToMono(String.class)
                .map(body -> new RuntimeException(errorPrefix + body));
    }

    private Mono<ResponseEntity<Void>> rollbackUserCreation(Long userId, Throwable throwable) {
        return userWebClient.delete()
                .uri("/api/users/" + userId)
                .retrieve()
                .toBodilessEntity()
                .then(Mono.error(new RuntimeException("Registration rolled back. Reason: " + throwable.getMessage())));
    }
}