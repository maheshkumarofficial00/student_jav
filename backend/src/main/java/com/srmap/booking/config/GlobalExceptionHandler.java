package com.srmap.booking.config;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

/**
 * Converts domain exceptions to the JSON format `{ "message": "..." }`
 * that the frontend's ApiError class expects.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You do not have access to this."));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Invalid request body format."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(m -> m != null && !m.isBlank())
                .findFirst()
                .orElse("Validation failed.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An unexpected error occurred. Please try again."));
    }
}
