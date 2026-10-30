package com.example.warehouse.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.warehouse.dto.response.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Translates exceptions to RFC 7807 problem responses. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage(), null);
    }

    /** e.g. GET /api/items?sort=nonExistingField -> 400 instead of 500. */
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<Object> handleUnknownSortProperty(PropertyReferenceException ex) {
        return build(HttpStatus.BAD_REQUEST, "Unknown sort property: " + ex.getPropertyName(), null);
    }

    /** Safety net for unique-constraint races that slip past the existsBy checks. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "Data integrity violation (duplicate SKU?)", null);
    }

    /** Last resort: never leak stack traces or internal messages to the client. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", null);
    }

    /** All other Spring MVC exceptions (404 no resource, 405, 415, type mismatch, ...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        String message = body instanceof ProblemDetail pd && pd.getDetail() != null
                ? pd.getDetail()
                : "Request failed";
        return ResponseEntity.status(statusCode).headers(headers).body(BaseResponse.failed(message));
    }

    private ResponseEntity<Object> build(HttpStatus status, String message, Object data) {
        return ResponseEntity.status(status).body(BaseResponse.failed(message, data));
    }
}
