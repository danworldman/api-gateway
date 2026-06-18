package com.innowise.apigateway.integration;

import com.innowise.apigateway.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegistrationControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_shouldReturn201_whenUserAndAuthServicesSucceed() {
        userMockServer.stubFor(post(urlEqualTo("/api/users"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":1}")));

        authMockServer.stubFor(post(urlEqualTo("/api/v1/auth/register"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{}")));

        RegisterRequest request = new RegisterRequest(
                "Ivan", "Petrov", LocalDate.of(1990, 5, 15),
                "ivan@mail.com", "ivan_user", "securePass123", "USER"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                entity,
                String.class
        );

        // Проверяем статус ответа (CREATED или OK в зависимости от маппинга контроллера шлюза)
        assertThat(response.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);

        // Явно вызываем верификацию на конкретных инстансах серверов WireMock
        userMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/users")));
        authMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/v1/auth/register")));
    }

    @Test
    void register_shouldRollback_whenAuthServiceFails() {
        userMockServer.stubFor(post(urlEqualTo("/api/users"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":1}")));

        authMockServer.stubFor(post(urlEqualTo("/api/v1/auth/register"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"Auth error\"}")));

        userMockServer.stubFor(delete(urlEqualTo("/api/users/1"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

        RegisterRequest request = new RegisterRequest(
                "Ivan", "Petrov", LocalDate.of(1990, 5, 15),
                "ivan@mail.com", "ivan_user", "securePass123", "USER"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

        HttpServerErrorException exception = assertThrows(
                HttpServerErrorException.class,
                () -> restTemplate.postForEntity(
                        baseUrl() + "/api/v1/auth/register",
                        entity,
                        String.class
                )
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        userMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/users")));
        authMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/v1/auth/register")));
        userMockServer.verify(exactly(1), deleteRequestedFor(urlEqualTo("/api/users/1")));
    }

    @Test
    void register_shouldReturn500_whenUserServiceFails() {
        userMockServer.stubFor(post(urlEqualTo("/api/users"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"Bad request\"}")));

        RegisterRequest request = new RegisterRequest(
                "Ivan", "Petrov", LocalDate.of(1990, 5, 15),
                "ivan@mail.com", "ivan_user", "securePass123", "USER"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

        HttpServerErrorException exception = assertThrows(
                HttpServerErrorException.class,
                () -> restTemplate.postForEntity(
                        baseUrl() + "/api/v1/auth/register",
                        entity,
                        String.class
                )
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        userMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/users")));
        authMockServer.verify(0, postRequestedFor(urlEqualTo("/api/v1/auth/register")));
    }
}