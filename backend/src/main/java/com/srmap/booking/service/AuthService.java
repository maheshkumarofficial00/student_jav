package com.srmap.booking.service;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.AuthResponse;
import com.srmap.booking.dto.LoginRequest;
import com.srmap.booking.dto.RegisterRequest;
import com.srmap.booking.dto.UserDto;
import com.srmap.booking.entity.User;
import com.srmap.booking.repository.UserRepository;
import com.srmap.booking.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@srmap\\.edu\\.in$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[^A-Za-z0-9]");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < 8 ||
                !UPPERCASE_PATTERN.matcher(password).find() ||
                !LOWERCASE_PATTERN.matcher(password).find() ||
                !DIGIT_PATTERN.matcher(password).find() ||
                !SPECIAL_PATTERN.matcher(password).find()) {
            throw ApiException.badRequest("Password must have at least 8 characters, uppercase, lowercase, a number and a special character.");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw ApiException.badRequest("Use an email ending in @srmap.edu.in.");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        String password = req.getPassword() != null ? req.getPassword() : "";

        validateEmail(email);
        validatePassword(password);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Email or password is incorrect."));

        if (!user.isActive() || !passwordEncoder.matches(password, user.getPassword())) {
            throw ApiException.unauthorized("Email or password is incorrect.");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String name = req.getName() != null ? req.getName().trim() : "";
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        String password = req.getPassword() != null ? req.getPassword() : "";

        if (name.length() < 2) {
            throw ApiException.badRequest("Enter your full name.");
        }

        validateEmail(email);
        validatePassword(password);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists.");
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(User.Role.STUDENT)
                .active(true)
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("User not found."));
        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentUserEntity(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Please sign in again."));
    }
}
