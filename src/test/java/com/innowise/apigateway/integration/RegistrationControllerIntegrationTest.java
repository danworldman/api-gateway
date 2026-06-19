package com.innowise.apigateway.integration;

import com.innowise.apigateway.dto.auth.RegisterRequest;
import com.innowise.apigateway.testdata.GatewayTestData;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegistrationControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldReturn201_whenUserAndAuthServicesSucceed() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(defaultRegisterRequest, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);
        userMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/users")));
        authMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/v1/auth/register")));
    }

    @Test
    void shouldRollback_whenAuthServiceFails() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(defaultRegisterRequest, headers);

        assertThatThrownBy(() -> restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                entity,
                String.class
        ))
                .isInstanceOf(HttpServerErrorException.class)
                .extracting(e -> ((HttpServerErrorException) e).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        userMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/users")));
        authMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/v1/auth/register")));
        userMockServer.verify(exactly(1), deleteRequestedFor(urlEqualTo("/api/users/1")));
    }

    @Test
    void shouldReturn500_whenUserServiceFails() {
        userMockServer.stubFor(post(urlEqualTo("/api/users"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"Bad request\"}")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(defaultRegisterRequest, headers);

        assertThatThrownBy(() -> restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                entity,
                String.class
        ))
                .isInstanceOf(HttpServerErrorException.class)
                .extracting(e -> ((HttpServerErrorException) e).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        userMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/api/users")));
        authMockServer.verify(0, postRequestedFor(urlEqualTo("/api/v1/auth/register")));
    }

    private final RegisterRequest defaultRegisterRequest = new RegisterRequest(
            GatewayTestData.DEFAULT_USER_NAME,
            GatewayTestData.DEFAULT_USER_SURNAME,
            GatewayTestData.DEFAULT_BIRTH_DATE,
            GatewayTestData.DEFAULT_USER_EMAIL,
            GatewayTestData.DEFAULT_USERNAME,
            GatewayTestData.DEFAULT_PASSWORD,
            GatewayTestData.DEFAULT_ROLE
    );
}