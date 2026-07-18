package com.innowise.apigateway.testdata;

import com.innowise.apigateway.dto.auth.RegisterRequest;
import com.innowise.apigateway.dto.user.UserResponse;
import com.innowise.apigateway.dto.auth.ValidateResponse;

import java.time.LocalDate;

public abstract class GatewayTestData {

    public static final Long DEFAULT_USER_ID = 1L;
    public static final String DEFAULT_USER_NAME = "Bob";
    public static final String DEFAULT_USER_SURNAME = "Duck";
    public static final String DEFAULT_USER_EMAIL = "bob@email.com";
    public static final LocalDate DEFAULT_BIRTH_DATE = LocalDate.of(2000, 5, 19);
    public static final String DEFAULT_USERNAME = "bob_user";
    public static final String DEFAULT_PASSWORD = "password";
    public static final String DEFAULT_ROLE = "USER";
    public static final String VALID_TOKEN = "valid_token";

    protected final RegisterRequest defaultRegisterRequest;
    protected final UserResponse defaultUserResponse;
    protected final ValidateResponse defaultValidateResponse;

    protected GatewayTestData() {
        defaultRegisterRequest = new RegisterRequest(
                DEFAULT_USER_NAME,
                DEFAULT_USER_SURNAME,
                DEFAULT_BIRTH_DATE,
                DEFAULT_USER_EMAIL,
                DEFAULT_USERNAME,
                DEFAULT_PASSWORD,
                DEFAULT_ROLE
        );
        defaultUserResponse = new UserResponse(DEFAULT_USER_ID);
        defaultValidateResponse = new ValidateResponse(DEFAULT_USER_ID, DEFAULT_ROLE);
    }
}