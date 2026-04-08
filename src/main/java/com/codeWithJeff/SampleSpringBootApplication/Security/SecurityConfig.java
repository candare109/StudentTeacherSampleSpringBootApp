package com.codeWithJeff.SampleSpringBootApplication.Security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures Spring Security for JWT authentication.
 *
 * KEY DECISIONS MADE HERE:
 *   1. Which endpoints are public (no token needed)?
 *   2. Which endpoints require authentication?
 *   3. Sessions are STATELESS (no server-side sessions — JWT handles it)
 *   4. Our JwtAuthenticationFilter runs before Spring's default filter
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Password encoder — BCrypt hashes passwords so they're never stored in plain text.
     * Used in: AuthController (register) and AuthenticationManager (login verification)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager — Spring Security's built-in component that
     * verifies username + password during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * The main security configuration.
     *
     * READ THIS like a rulebook:
     *   - /api/auth/** → OPEN (anyone can register/login)
     *   - /swagger-ui/**, /v3/api-docs/** → OPEN (API docs accessible)
     *   - /h2-console/** → OPEN (H2 database console for dev)
     *   - /test → OPEN (your test endpoint)
     *   - Everything else → REQUIRES a valid JWT token
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — not needed for stateless JWT APIs
                .csrf(csrf -> csrf.disable())

                // Allow H2 console to load in iframes
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // Define endpoint access rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no token needed
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/test").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // (Optional) Role-based example: only ADMIN can delete
                        // .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Stateless sessions — no server-side session, JWT handles authentication
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Add our JWT filter BEFORE Spring's default username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}