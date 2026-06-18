package com.innowise.apigateway.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    protected RestTemplate restTemplate;

    protected static final WireMockServer userMockServer;
    protected static final WireMockServer authMockServer;

    static {
        userMockServer = new WireMockServer(0);
        userMockServer.start();

        authMockServer = new WireMockServer(0);
        authMockServer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> "http://localhost:" + authMockServer.port());
        registry.add("USER_SERVICE_URL", () -> "http://localhost:" + userMockServer.port());
    }

    @BeforeEach
    void setUpBase() {
        restTemplate = new RestTemplate();
        userMockServer.resetRequests();
        userMockServer.resetToDefaultMappings();
        authMockServer.resetRequests();
        authMockServer.resetToDefaultMappings();
    }

    @AfterEach
    void tearDownBase() {
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}