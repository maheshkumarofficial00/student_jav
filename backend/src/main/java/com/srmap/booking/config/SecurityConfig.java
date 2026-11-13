package com.srmap.booking.config;

import com.srmap.booking.security.JwtAuthFilter;
import com.srmap.booking.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final SecurityErrorHandler securityErrorHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsServiceImpl userDetailsService, SecurityErrorHandler securityErrorHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toList();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Allow preflight CORS OPTIONS requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public auth endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()

                // Faculty schedule management requires FACULTY or ADMIN (must precede general /api/faculty/**)
                .requestMatchers("/api/faculty/*/schedule/**", "/api/faculty/*/schedule",
                                 "/api/faculty/{id}/schedule/**", "/api/faculty/{id}/schedule").hasAnyRole("FACULTY", "ADMIN")

                // Public faculty directory & availability
                .requestMatchers(HttpMethod.GET, "/api/faculty", "/api/faculty/**").permitAll()

                // Public room catalog & availability
                .requestMatchers(HttpMethod.GET, "/api/rooms", "/api/rooms/**").permitAll()

                // Public resource catalog & availability
                .requestMatchers(HttpMethod.GET, "/api/resources", "/api/resources/**").permitAll()

                // Combined availability search is open
                .requestMatchers(HttpMethod.POST, "/api/availability/combined").permitAll()

                // Admin endpoints (require ADMIN role)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Student appointment booking and viewing
                .requestMatchers(HttpMethod.POST, "/api/appointments").hasRole("STUDENT")
                .requestMatchers(HttpMethod.GET, "/api/appointments/mine").hasRole("STUDENT")

                // Faculty appointment management
                .requestMatchers(HttpMethod.GET, "/api/appointments/incoming").hasRole("FACULTY")
                .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/status", "/api/appointments/{id}/status").hasRole("FACULTY")

                // Appointment cancellation (student owner, faculty owner, or admin)
                .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/cancel", "/api/appointments/{id}/cancel").authenticated()

                // Room / resource reservations
                .requestMatchers(HttpMethod.POST, "/api/bookings").hasAnyRole("STUDENT", "FACULTY", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/bookings/mine").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/cancel", "/api/bookings/{id}/cancel").authenticated()

                // Current authenticated user profile
                .requestMatchers("/api/auth/me").authenticated()

                // Everything else requires authentication
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
