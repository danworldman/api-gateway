package com.innowise.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient authWebClient(@Value("${AUTH_SERVICE_URL:http://localhost:8081}") String authUrl) {
        return WebClient.builder()
                .baseUrl(authUrl)
                .build();
    }

    @Bean
    public WebClient userWebClient(@Value("${USER_SERVICE_URL:http://localhost:8082}") String userUrl) {
        return WebClient.builder()
                .baseUrl(userUrl)
                .build();
    }
}