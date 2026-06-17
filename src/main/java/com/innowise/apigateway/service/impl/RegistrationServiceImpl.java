package com.innowise.apigateway.service.impl;

import com.innowise.apigateway.dto.RegisterRequest;
import com.innowise.apigateway.service.RegistrationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    private final WebClient authWebClient;
    private final WebClient userWebClient;

    @Override
    public Mono<Void> register(RegisterRequest request) {
        UserCreateRequestJson userDto = new UserCreateRequestJson(
                request.name(),
                request.surname(),
                request.birthDate(),
                request.email()
        );

        return userWebClient.post()
                .uri("/api/users")
                .bodyValue(userDto)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("User service error: " + errorBody)))
                )
                .bodyToMono(UserResponseJson.class)
                .flatMap(userResponse -> {
                    Long userId = userResponse.getId();

                    AuthRegisterRequestJson authDto = new AuthRegisterRequestJson(
                            userId,
                            request.username(),
                            request.password(),
                            request.role()
                    );

                    return authWebClient.post()
                            .uri("/api/v1/auth/register")
                            .bodyValue(authDto)
                            .retrieve()
                            .onStatus(status -> status.isError(), response ->
                                    response.bodyToMono(String.class)
                                            .flatMap(errorBody -> Mono.error(new RuntimeException(errorBody)))
                            )
                            .toBodilessEntity()
                            .then()
                            .onErrorResume(Throwable.class, e -> {
                                log.error("Auth service failed, rolling back user in User Service for id: {}", userId);
                                return userWebClient.delete()
                                        .uri("/api/users/" + userId)
                                        .retrieve()
                                        .toBodilessEntity()
                                        .then(authWebClient.post()
                                                .uri("/api/v1/auth/rollback/" + userId)
                                                .retrieve()
                                                .toBodilessEntity())
                                        .then(Mono.error(new RuntimeException("Registration rolled back: " + e.getMessage())));
                            });
                });
    }

    @Data
    @AllArgsConstructor
    static class UserCreateRequestJson {
        private String name;
        private String surname;
        private LocalDate birthDate;
        private String email;
    }

    @Data
    @AllArgsConstructor
    static class AuthRegisterRequestJson {
        private Long userServiceId;
        private String username;
        private String password;
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class UserResponseJson {
        private Long id;
    }
}