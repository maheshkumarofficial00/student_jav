package com.srmap.booking.config;

import org.springframework.http.HttpStatus;

/**
 * Domain exception that carries an HTTP status code.
 * Caught by GlobalExceptionHandler and converted to { "message": "..." }.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // ---- convenience factories ----
    public static ApiException badRequest(String msg)   { return new ApiException(HttpStatus.BAD_REQUEST, msg); }
    public static ApiException unauthorized(String msg) { return new ApiException(HttpStatus.UNAUTHORIZED, msg); }
    public static ApiException forbidden(String msg)    { return new ApiException(HttpStatus.FORBIDDEN, msg); }
    public static ApiException notFound(String msg)     { return new ApiException(HttpStatus.NOT_FOUND, msg); }
    public static ApiException conflict(String msg)     { return new ApiException(HttpStatus.CONFLICT, msg); }
}
