package com.codeWithJeff.SampleSpringBootApplication.Controller;

import com.codeWithJeff.SampleSpringBootApplication.Entity.User;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceAlreadyExistsException;
import com.codeWithJeff.SampleSpringBootApplication.Repository.UserRepository;
import com.codeWithJeff.SampleSpringBootApplication.Security.JwtService;
import com.codeWithJeff.SampleSpringBootApplication.dto.AuthRequest;
import com.codeWithJeff.SampleSpringBootApplication.dto.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user registration and login.
 * These endpoints are PUBLIC (configured in SecurityConfig).
 *
 * FLOW:
 *   POST /api/auth/register → create user → return JWT token
 *   POST /api/auth/login    → verify credentials → return JWT token
 */

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user.
     *
     * 1. Check if email already exists → 409 Conflict
     * 2. Hash password with BCrypt (NEVER store plain text passwords)
     * 3. Save user to database
     * 4. Generate JWT token
     * 5. Return token to client
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .role("USER")                                             // default role
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
    /**
     * Login with existing credentials.
     *
     * 1. AuthenticationManager verifies email + password against DB
     *    (uses CustomUserDetailsService + BCryptPasswordEncoder internally)
     * 2. If credentials are wrong → Spring throws BadCredentialsException → 401
     * 3. If correct → generate JWT token → return to client
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {

        // This line does ALL the verification:
        //   - Calls CustomUserDetailsService.loadUserByUsername(email)
        //   - Compares BCrypt hash of provided password vs stored hash
        //   - Throws BadCredentialsException if wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }


}
