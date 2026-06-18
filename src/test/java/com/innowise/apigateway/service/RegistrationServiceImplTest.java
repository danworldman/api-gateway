package com.innowise.apigateway.service;

import com.innowise.apigateway.dto.RegisterRequest;
import com.innowise.apigateway.service.impl.RegistrationServiceImpl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;

class RegistrationServiceImplTest {

    private MockWebServer userMockServer;
    private MockWebServer authMockServer;
    private RegistrationServiceImpl registrationService;

    @BeforeEach
    void setUp() throws IOException {
        userMockServer = new MockWebServer();
        userMockServer.start();
        authMockServer = new MockWebServer();
        authMockServer.start();

        WebClient userWebClient = WebClient.builder()
                .baseUrl(userMockServer.url("/").toString())
                .build();
        WebClient authWebClient = WebClient.builder()
                .baseUrl(authMockServer.url("/").toString())
                .build();

        registrationService = new RegistrationServiceImpl(authWebClient, userWebClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        userMockServer.shutdown();
        authMockServer.shutdown();
    }

    @Test
    void register_ShouldSucceed_WhenUserAndAuthServicesSucceed() {
        RegisterRequest request = new RegisterRequest(
                "Ivan", "Petrov", LocalDate.of(1990, 5, 15),
                "ivan@mail.com", "ivan_user", "securePass123", "USER"
        );

        userMockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":1}"));

        authMockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        Mono<Void> result = registrationService.register(request);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void register_ShouldRollback_WhenAuthServiceFails() {
        RegisterRequest request = new RegisterRequest(
                "Ivan", "Petrov", LocalDate.of(1990, 5, 15),
                "ivan@mail.com", "ivan_user", "securePass123", "USER"
        );

        userMockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":1}"));

        authMockServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Auth error\"}"));

        userMockServer.enqueue(new MockResponse()
                .setResponseCode(204)
                .setHeader("Content-Type", "application/json")
                .setBody(""));

        Mono<Void> result = registrationService.register(request);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("Registration rolled back"))
                .verify();
    }

    @Test
    void register_ShouldFail_WhenUserServiceFails() {
        RegisterRequest request = new RegisterRequest(
                "Ivan", "Petrov", LocalDate.of(1990, 5, 15),
                "ivan@mail.com", "ivan_user", "securePass123", "USER"
        );

        userMockServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Bad request\"}"));

        Mono<Void> result = registrationService.register(request);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("User service error"))
                .verify();
    }
}