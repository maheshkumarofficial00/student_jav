package com.srmap.booking.controller;

import com.srmap.booking.dto.AuthResponse;
import com.srmap.booking.dto.LoginRequest;
import com.srmap.booking.dto.RegisterRequest;
import com.srmap.booking.dto.UserDto;
import com.srmap.booking.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(authService.getCurrentUser(email));
    }
}
