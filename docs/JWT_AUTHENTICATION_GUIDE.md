# 🔐 JWT Authentication Guide for SampleSpringBootApplication

A step-by-step guide to adding **JWT (JSON Web Token) authentication** to this Spring Boot project.
Read this first to understand how JWT works, then follow the implementation steps.

---

## 📑 Table of Contents

- [What is JWT?](#-what-is-jwt)
- [How JWT Works (The Flow)](#-how-jwt-works-the-flow)
- [How It Applies to This Project](#-how-it-applies-to-this-project)
- [What You Need to Create](#-what-you-need-to-create)
- [Step 1 — Add Dependencies](#step-1--add-dependencies)
- [Step 2 — Add JWT Properties](#step-2--add-jwt-properties)
- [Step 3 — Create the User Entity](#step-3--create-the-user-entity)
- [Step 4 — Create the User Repository](#step-4--create-the-user-repository)
- [Step 5 — Create Auth DTOs](#step-5--create-auth-dtos)
- [Step 6 — Create JwtService (Token Utility)](#step-6--create-jwtservice-token-utility)
- [Step 7 — Create CustomUserDetailsService](#step-7--create-customuserdetailsservice)
- [Step 8 — Create JwtAuthenticationFilter](#step-8--create-jwtauthenticationfilter)
- [Step 9 — Create SecurityConfig](#step-9--create-securityconfig)
- [Step 10 — Create AuthController](#step-10--create-authcontroller)
- [Step 11 — Update GlobalExceptionHandler](#step-11--update-globalexceptionhandler)
- [Testing with Postman / curl](#-testing-with-postman--curl)
- [Understanding the Request Flow with JWT](#-understanding-the-request-flow-with-jwt)
- [Checklist Summary](#-checklist-summary)
- [Common Mistakes](#-common-mistakes)

---

## 📖 What is JWT?

A **JSON Web Token (JWT)** is a compact, URL-safe string that proves who you are. It has 3 parts separated by dots:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqZWZmQGVtYWlsLmNvbSIsImlhdCI6MTcxMjQ1NjAwMH0.abc123signature
|______ HEADER ______| |________________ PAYLOAD ________________| |____ SIGNATURE ____|
```

| Part | Contains | Example |
|------|----------|---------|
| **Header** | Algorithm + token type | `{"alg": "HS256", "typ": "JWT"}` |
| **Payload** | User data (claims) — username, role, expiry | `{"sub": "jeff@email.com", "role": "ADMIN", "exp": 1712456000}` |
| **Signature** | Verification hash using your secret key | Prevents tampering — if anyone changes the payload, the signature won't match |

**Key idea**: The server never stores the token. It signs the token with a secret key, and later verifies incoming tokens using that same key. This is called **stateless authentication** (no sessions stored on the server).

---

## 🔄 How JWT Works (The Flow)

```
┌──────────┐                          ┌──────────────┐                    ┌────────────┐
│  Client   │                          │  Spring Boot │                    │  Database   │
│ (Postman) │                          │    Server    │                    │  (H2/PG)   │
└─────┬─────┘                          └──────┬───────┘                    └─────┬──────┘
      │                                       │                                  │
      │  1. POST /api/auth/register           │                                  │
      │  { "email", "password" }              │                                  │
      │ ─────────────────────────────────────► │                                  │
      │                                       │  2. Hash password with BCrypt    │
      │                                       │  3. Save user ──────────────────►│
      │                                       │                                  │
      │  4. Return: { "token": "eyJhb..." }   │                                  │
      │ ◄──────────────────────────────────── │                                  │
      │                                       │                                  │
      │  5. GET /api/students                 │                                  │
      │  Header: Authorization: Bearer eyJhb..│                                  │
      │ ─────────────────────────────────────► │                                  │
      │                                       │  6. JwtAuthenticationFilter:      │
      │                                       │     - Extract token from header   │
      │                                       │     - Validate signature + expiry │
      │                                       │     - Load user from DB ─────────►│
      │                                       │     - Set SecurityContext          │
      │                                       │                                  │
      │                                       │  7. Controller executes normally  │
      │  8. Return: [ { student data } ]      │                                  │
      │ ◄──────────────────────────────────── │                                  │
      │                                       │                                  │
      │  9. GET /api/students (NO token)      │                                  │
      │ ─────────────────────────────────────► │                                  │
      │                                       │  10. Filter sees no token         │
      │  11. Return: 401 Unauthorized         │     → rejects request            │
      │ ◄──────────────────────────────────── │                                  │
```

### In plain English:
1. **Register/Login** → Client sends credentials → Server returns a signed JWT token
2. **Access protected endpoint** → Client sends the token in the `Authorization` header → Server validates it → Request proceeds
3. **No token or invalid token** → Server returns `401 Unauthorized`

---

## 🎯 How It Applies to This Project

Right now, **all your endpoints are open** — anyone can call `POST /api/students` or `DELETE /api/teachers/1` without any authentication. With JWT, you can protect them:

| Endpoint | Before (Now) | After (With JWT) |
|----------|-------------|-------------------|
| `POST /api/auth/register` | ❌ Doesn't exist | 🟢 Public — creates user + returns token |
| `POST /api/auth/login` | ❌ Doesn't exist | 🟢 Public — validates credentials + returns token |
| `GET /api/students` | 🟢 Open to anyone | 🔒 Requires valid JWT token |
| `POST /api/subjects` | 🟢 Open to anyone | 🔒 Requires valid JWT token |
| `DELETE /api/teachers/{id}` | 🟢 Open to anyone | 🔒 Requires valid JWT token (optionally ADMIN only) |
| `GET /test` | 🟢 Open to anyone | 🟢 Can keep public |
| `GET /swagger-ui.html` | 🟢 Open to anyone | 🟢 Keep public for dev |
| `GET /h2-console` | 🟢 Open to anyone | 🟢 Keep public for dev |

### New files you'll create:

```
src/main/java/com/codeWithJeff/SampleSpringBootApplication/
├── Entity/
│   └── User.java                          ← NEW: User entity (email, password, role)
├── Repository/
│   └── UserRepository.java                ← NEW: Find user by email
├── dto/
│   ├── AuthRequest.java                   ← NEW: Login/register request body
│   └── AuthResponse.java                  ← NEW: Returns the JWT token
├── Security/
│   ├── JwtService.java                    ← NEW: Generate + validate JWT tokens
│   ├── JwtAuthenticationFilter.java       ← NEW: Intercepts every request, checks token
│   ├── CustomUserDetailsService.java      ← NEW: Loads user from DB for Spring Security
│   └── SecurityConfig.java                ← NEW: Configures which endpoints are public/protected
├── Controller/
│   └── AuthController.java                ← NEW: /api/auth/register and /api/auth/login
```

---

## 🔨 What You Need to Create

Here's every file, in the order you should create them.

---

### Step 1 — Add Dependencies

Add these to your `build.gradle` inside `dependencies { }`:

```groovy
dependencies {
    // ...existing dependencies...

    // Spring Security — provides authentication/authorization framework
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // JJWT — library to create and parse JWT tokens
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
}
```

Then run:
```powershell
.\gradlew build --refresh-dependencies
```

> **What each dependency does:**
> - `spring-boot-starter-security` — Adds Spring Security. Once added, **ALL endpoints become protected by default** (returns 401). That's why you need SecurityConfig to whitelist public endpoints.
> - `jjwt-api` / `jjwt-impl` / `jjwt-jackson` — The [JJWT library](https://github.com/jwtk/jjwt) for creating, signing, and parsing JWT tokens.

---

### Step 2 — Add JWT Properties

Add to `application.yml`:

```yaml
# Add under the existing content
jwt:
  secret: my-super-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm-ok
  expiration: 86400000   # 24 hours in milliseconds
```

> **Why?** The secret key signs your tokens. The expiration controls how long a token is valid. In production, use environment variables instead of hardcoding.

---

### Step 3 — Create the User Entity

📄 `src/main/java/com/codeWithJeff/SampleSpringBootApplication/Entity/User.java`

```java
package com.codeWithJeff.SampleSpringBootApplication.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;   // stored as BCrypt hash, NEVER plain text

    @Column(nullable = false, length = 20)
    private String role;       // "USER" or "ADMIN"
}
```

> **How it connects:** This is just like your `Student` or `Teacher` entity. JPA creates a `users` table. Spring Security will look up users from this table when validating tokens.

---

### Step 4 — Create the User Repository

📄 `src/main/java/com/codeWithJeff/SampleSpringBootApplication/Repository/UserRepository.java`

```java
package com.codeWithJeff.SampleSpringBootApplication.Repository;

import com.codeWithJeff.SampleSpringBootApplication.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

> **Same pattern** as your `StudentRepository` and `SubjectRepository`.

---

### Step 5 — Create Auth DTOs

📄 `src/main/java/com/codeWithJeff/SampleSpringBootApplication/dto/AuthRequest.java`

```java
package com.codeWithJeff.SampleSpringBootApplication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
```

📄 `src/main/java/com/codeWithJeff/SampleSpringBootApplication/dto/AuthResponse.java`

```java
package com.codeWithJeff.SampleSpringBootApplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String role;
}
```

> **Same DTO pattern** you already use (e.g., `SubjectRequestDto` / `SubjectResponseDto`).

---

### Step 6 — Create JwtService (Token Utility)

📄 `src/main/java/com/codeWithJeff/SampleSpringBootApplication/Security/JwtService.java`

```java
package com.codeWithJeff.SampleSpringBootApplication.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility service that handles ALL JWT operations:
 *   1. generateToken()  — creates a signed JWT after successful login
 *   2. extractEmail()   — reads the "sub" (subject) claim from a token
 *   3. isTokenValid()   — checks signature + expiry
 *
 * FLOW:
 *   AuthController calls generateToken() → returns token string to client
 *   JwtAuthenticationFilter calls extractEmail() + isTokenValid() on every request
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Creates the signing key from the secret string.
     * JJWT requires a SecretKey object, not a raw string.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a new JWT token for the given email.
     *
     * The token contains:
     *   - sub (subject): the user's email
     *   - iat (issued at): current time
     *   - exp (expiration): current time + 24 hours
     *   - signature: HMAC-SHA256 hash using our secret key
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)                                           // who this token is for
                .claim("role", role)                                      // custom claim
                .issuedAt(new Date())                                     // when it was created
                .expiration(new Date(System.currentTimeMillis() + expiration))  // when it expires
                .signWith(getSigningKey())                                // sign with secret
                .compact();                                               // build the string
    }

    /**
     * Extract the email (subject claim) from a token.
     * Used by JwtAuthenticationFilter to identify which user is making the request.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Check if a token is valid:
     *   1. Does the email in the token match the expected email?
     *   2. Has the token expired?
     */
    public boolean isTokenValid(String token, String expectedEmail) {
        String email = extractEmail(token);
        return email.equals(expectedEmail) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Parse and verify the token using our signing key.
     * If the signature doesn't match or token is malformed, JJWT throws an exception.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())    // verify signature with our secret
                .build()
                .parseSignedClaims(token)       // parse and validate
                .getPayload();                  // return the claims (payload)
    }
}
```

> **Key concept:** This class is the heart of JWT. It uses the JJWT library to create tokens (on login) and validate them (on every request).

---

### Step 7 — Create CustomUserDetailsService

📄 `src/main/java/com/codeWithJeff/SampleSpringBootApplication/Security/CustomUserDetailsService.java`

```java
package com.codeWithJeff.SampleSpringBootApplication.Security;

import com.codeWithJeff.SampleSpringBootApplication.Entity.User;
import com.codeWithJeff.SampleSpringBootApplication.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security needs a way to load user data from YOUR database.
 * This class bridges YOUR User entity ↔ Spring Security's UserDetails.
 *
 * FLOW:
 *   JwtAuthenticationFilter extracts email from token
 *   → calls this service to load the user from DB
 *   → Spring Security uses the returned UserDetails to set the security context
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
```

> **Why?** Spring Security doesn't know about your `User` entity. This adapter translates your entity into something Spring Security understands (`UserDetails`).

---

### Step 8 — Create JwtAuthenticationFilter

📄 `src/main/java/com/codeWithJeff/SampleSpringBootApplication/Security/JwtAuthenticationFilter.java`

```java
package com.codeWithJeff.SampleSpringBootApplication.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This filter runs ONCE for EVERY HTTP request (before your controllers).
 *
 * FLOW for each request:
 *   1. Check if "Authorization: Bearer <token>" header exists
 *   2. If no header → skip (let SecurityConfig decide if endpoint is public)
 *   3. If header exists → extract token → validate it
 *   4. If valid → load user from DB → tell Spring Security "this user is authenticated"
 *   5. If invalid → skip (Spring Security will return 401)
 *
 * ANALOGY:
 *   Think of this as a security guard at the door.
 *   Every person (request) walks past the guard.
 *   If they show a valid badge (JWT token), the guard lets them through.
 *   If no badge or fake badge, the guard blocks them.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Get the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. If no header or doesn't start with "Bearer ", skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);  // pass to next filter
            return;
        }

        // 3. Extract the token (remove "Bearer " prefix)
        final String token = authHeader.substring(7);

        // 4. Extract the email from the token
        final String email = jwtService.extractEmail(token);

        // 5. If we got an email AND user is not already authenticated in this request
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Load user from database
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // 7. Validate the token against the loaded user
            if (jwtService.isTokenValid(token, userDetails.getUsername())) {

                // 8. Create authentication token and set it in SecurityContext
                //    This tells Spring: "This request is from an authenticated user"
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // no credentials needed (already verified)
                                userDetails.getAuthorities()   // roles: ROLE_USER, ROLE_ADMIN
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 9. Continue to the next filter / controller
        filterChain.doFilter(request, response);
    }
}
```

> **This is the most important class.** It intercepts every HTTP request before it reaches your controllers. Without a valid JWT, protected endpoints return 401.

---

### Step 9 — Create SecurityConfig

📄 `src/main/java/com/codeWithJeff/SampleSpringBootApplication/Security/SecurityConfig.java`

```java
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
```

> **This is your "rulebook."** It decides what's public and what's protected. Customize the `requestMatchers` to fit your needs.

---

### Step 10 — Create AuthController

📄 `src/main/java/com/codeWithJeff/SampleSpringBootApplication/Controller/AuthController.java`

```java
package com.codeWithJeff.SampleSpringBootApplication.Controller;

import com.codeWithJeff.SampleSpringBootApplication.Entity.User;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceAlreadyExistsException;
import com.codeWithJeff.SampleSpringBootApplication.Repository.UserRepository;
import com.codeWithJeff.SampleSpringBootApplication.Security.JwtService;
import com.codeWithJeff.SampleSpringBootApplication.dto.AuthRequest;
import com.codeWithJeff.SampleSpringBootApplication.dto.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
```

> **Same controller pattern** as your `StudentController` — just for authentication endpoints.

---

### Step 11 — Update GlobalExceptionHandler

Add a handler for bad login credentials in your existing `GlobalExceptionHandler.java`:

```java
// Add this import
import org.springframework.security.authentication.BadCredentialsException;

// Add this method inside the class
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ErrorResponse> handleBadCredentials(
        BadCredentialsException ex, HttpServletRequest request) {

    ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message("Invalid email or password")
            .path(request.getRequestURI())
            .build();

    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
}
```

---

## 🧪 Testing with Postman / curl

### 1. Register a new user
```
POST http://localhost:7000/api/auth/register
Content-Type: application/json

{
    "email": "jeff@email.com",
    "password": "mypassword123"
}
```

**Response (201 Created):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqZWZmQGVtYWlsLmNvbSIs...",
    "email": "jeff@email.com",
    "role": "USER"
}
```

### 2. Login
```
POST http://localhost:7000/api/auth/login
Content-Type: application/json

{
    "email": "jeff@email.com",
    "password": "mypassword123"
}
```

**Response (200 OK):** Same format — returns a fresh token.

### 3. Access protected endpoint WITH token
```
GET http://localhost:7000/api/students
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqZWZmQGVtYWlsLmNvbSIs...
```

**Response (200 OK):** Returns your student list normally.

### 4. Access protected endpoint WITHOUT token
```
GET http://localhost:7000/api/students
(no Authorization header)
```

**Response (401 Unauthorized):** Blocked by Spring Security.

### Postman tip:
In Postman, go to the **Authorization** tab → select **Bearer Token** → paste the token from the register/login response.

---

## 🔀 Understanding the Request Flow with JWT

Here's how a `GET /api/students` request flows through the entire application with JWT:

```
Client sends: GET /api/students
               Header: Authorization: Bearer eyJhb...

        │
        ▼
┌─────────────────────────────┐
│  JwtAuthenticationFilter    │  ← Runs FIRST (before controller)
│                             │
│  1. Reads "Authorization"   │
│     header from request     │
│  2. Extracts token string   │
│  3. Calls JwtService to     │
│     extract email + validate│
│  4. Loads user from DB via  │
│     CustomUserDetailsService│
│  5. Sets SecurityContext     │
│     (user is authenticated) │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│  SecurityConfig rules       │  ← Checks: is this endpoint protected?
│                             │     /api/students → YES, requires auth
│  User IS authenticated      │     ✅ Allow through
│  (set by filter above)      │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│  StudentController          │  ← Your existing controller
│  getAllStudents()            │     Runs normally, returns data
└─────────────┬───────────────┘
              │
              ▼
         200 OK + JSON response
```

---

## ✅ Checklist Summary

| # | Task | File |
|---|------|------|
| 1 | Add security + JJWT dependencies | `build.gradle` |
| 2 | Add jwt.secret and jwt.expiration | `application.yml` |
| 3 | Create User entity | `Entity/User.java` |
| 4 | Create UserRepository | `Repository/UserRepository.java` |
| 5 | Create AuthRequest DTO | `dto/AuthRequest.java` |
| 6 | Create AuthResponse DTO | `dto/AuthResponse.java` |
| 7 | Create JwtService | `Security/JwtService.java` |
| 8 | Create CustomUserDetailsService | `Security/CustomUserDetailsService.java` |
| 9 | Create JwtAuthenticationFilter | `Security/JwtAuthenticationFilter.java` |
| 10 | Create SecurityConfig | `Security/SecurityConfig.java` |
| 11 | Create AuthController | `Controller/AuthController.java` |
| 12 | Update GlobalExceptionHandler | `Exceptions/GlobalExceptionHandler.java` |

---

## ⚠️ Common Mistakes

| Mistake | What Happens | Fix |
|---------|-------------|-----|
| Forget to add `spring-boot-starter-security` | Nothing works, no `AuthenticationManager` bean | Add the dependency |
| Secret key too short | `WeakKeyException` at runtime | Use a secret that's at least 32 characters (256 bits) |
| Forget `Bearer ` prefix in header | Filter skips token, returns 401 | Always send `Authorization: Bearer <token>` |
| Don't whitelist `/api/auth/**` in SecurityConfig | Can't register or login (401) | Add `.requestMatchers("/api/auth/**").permitAll()` |
| Store plain text passwords | Massive security risk | Always use `passwordEncoder.encode()` |
| Forget to add `@Component` on filter | Filter never runs, all requests pass through | Add `@Component` on `JwtAuthenticationFilter` |
| Don't disable CSRF | POST/PUT/DELETE return 403 Forbidden | Add `.csrf(csrf -> csrf.disable())` in SecurityConfig |
| H2 console doesn't load | Blocked by Spring Security + frame options | Whitelist `/h2-console/**` + disable frame options |

