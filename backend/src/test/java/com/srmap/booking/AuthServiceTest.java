package com.srmap.booking;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.AuthResponse;
import com.srmap.booking.dto.LoginRequest;
import com.srmap.booking.dto.RegisterRequest;
import com.srmap.booking.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest();
        req.setEmail("student1@srmap.edu.in");
        req.setPassword("Campus@123");

        AuthResponse res = authService.login(req);
        assertNotNull(res.getToken());
        assertEquals("student1@srmap.edu.in", res.getUser().getEmail());
        assertEquals("STUDENT", res.getUser().getRole());
    }

    @Test
    void testLoginInvalidEmailFormat() {
        LoginRequest req = new LoginRequest();
        req.setEmail("student1@gmail.com");
        req.setPassword("Campus@123");

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(req));
        assertEquals("Use an email ending in @srmap.edu.in.", ex.getMessage());
    }

    @Test
    void testLoginInvalidPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("student1@srmap.edu.in");
        req.setPassword("WrongPassword@123");

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(req));
        assertEquals("Email or password is incorrect.", ex.getMessage());
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test Student");
        req.setEmail("teststudent@srmap.edu.in");
        req.setPassword("Campus@123");

        AuthResponse res = authService.register(req);
        assertNotNull(res.getToken());
        assertEquals("teststudent@srmap.edu.in", res.getUser().getEmail());
        assertEquals("STUDENT", res.getUser().getRole());
    }

    @Test
    void testRegisterDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Duplicate");
        req.setEmail("student1@srmap.edu.in");
        req.setPassword("Campus@123");

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(req));
        assertEquals("An account with this email already exists.", ex.getMessage());
    }
}
