package com.srmap.booking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.Map;

/**
 * Returns JSON error responses for 401 and 403 so the frontend
 * ApiError class can read the `message` field.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e)
            throws IOException {
        writeJson(res, 401, "Please sign in again.");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e)
            throws IOException {
        writeJson(res, 403, "You do not have access to this.");
    }

    private void writeJson(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getWriter(), Map.of("message", message));
    }
}
