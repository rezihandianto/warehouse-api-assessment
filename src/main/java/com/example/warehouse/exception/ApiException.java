package com.example.warehouse.exception;

import org.springframework.http.HttpStatus;

/** Business error carrying the HTTP status it should be translated to. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException notFound(String what, Object id) {
        return new ApiException(HttpStatus.NOT_FOUND, what + " not found: " + id);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException insufficientStock(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
