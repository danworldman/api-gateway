package com.innowise.apigateway.service;

import com.innowise.apigateway.dto.user.UserResponse;
import com.innowise.apigateway.service.impl.RegistrationServiceImpl;
import com.innowise.apigateway.testdata.GatewayTestData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

class RegistrationServiceImplTest extends GatewayTestData {

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
    void shouldRegisterSucceed_whenUserAndAuthServicesSucceed() {
        userMockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":1}"));

        authMockServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        Mono<UserResponse> result = registrationService.register(defaultRegisterRequest);

        StepVerifier.create(result)
                .expectNextMatches(userResponse -> userResponse.id() == 1L)
                .verifyComplete();
    }

    @Test
    void shouldRollback_whenAuthServiceFails() {
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

        Mono<UserResponse> result = registrationService.register(defaultRegisterRequest);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("Registration rolled back"))
                .verify();
    }

    @Test
    void shouldFail_whenUserServiceFails() {
        userMockServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Bad request\"}"));

        Mono<UserResponse> result = registrationService.register(defaultRegisterRequest);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e.getMessage().contains("User service error"))
                .verify();
    }
}