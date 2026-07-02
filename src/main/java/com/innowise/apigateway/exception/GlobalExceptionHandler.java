package com.innowise.apigateway.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Order(-2)
@Component
public class GlobalExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        String detail = "Internal server error";
        Map<String, Object> properties = new HashMap<>();

        if (throwable instanceof WebExchangeBindException bindException) {
            status = HttpStatus.BAD_REQUEST;
            detail = "Validation failed";
            Map<String, String> errors = new HashMap<>();
            bindException.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            properties.put("errors", errors);
        } else if (throwable instanceof WebClientResponseException webClientResponseException) {
            status = webClientResponseException.getStatusCode();
            String responseBody = webClientResponseException.getResponseBodyAsString();
            detail = !responseBody.isBlank() ? responseBody : webClientResponseException.getMessage();
        } else if (throwable instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            detail = throwable.getMessage();
        } else if (throwable.getMessage() != null) {
            detail = throwable.getMessage();
        }

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(throwable);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle("Gateway Execution Error");
        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));
        properties.forEach(problemDetail::setProperty);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        problemDetail.toString().getBytes(StandardCharsets.UTF_8)
                ))
        );
    }
}