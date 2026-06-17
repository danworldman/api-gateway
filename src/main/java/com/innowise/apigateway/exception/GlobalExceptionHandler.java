package com.innowise.apigateway.exception;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Order(-2)
@Component
public class GlobalExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String detail = "Internal server error";

        if (ex instanceof WebClientResponseException wcEx) {
            status = (HttpStatus) wcEx.getStatusCode();
            String responseBody = wcEx.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isEmpty()) {
                detail = responseBody;
            } else {
                detail = wcEx.getMessage();
            }
        } else if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            detail = ex.getMessage();
        } else if (ex.getMessage() != null) {
            detail = ex.getMessage();
        }

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = "{\"error\":\"" + detail.replace("\"", "\\\"") + "\"}";
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();

        return exchange.getResponse().writeWith(Mono.just(bufferFactory.wrap(json.getBytes(StandardCharsets.UTF_8))));
    }
}