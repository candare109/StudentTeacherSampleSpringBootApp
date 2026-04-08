# 🛡️ Role-Based Access Control Guide — JWT + Spring Security

**Last Updated:** April 8, 2026

A complete guide to implementing role-based access control (RBAC) in your Spring Boot project.
Covers: registering users with roles, protecting endpoints by role, method-level security, seeding an admin, and testing with Swagger UI.

---

## 📑 Table of Contents

- [How Roles Work in Spring Security](#-how-roles-work-in-spring-security)
- [The Role Flow (End to End)](#-the-role-flow-end-to-end)
- [Current State — What You Already Have](#-current-state--what-you-already-have)
- [Step 1 — Fix the Role Prefix Bug](#step-1--fix-the-role-prefix-bug)
- [Step 2 — Register Users with Specific Roles](#step-2--register-users-with-specific-roles)
- [Step 3 — Protect Endpoints by Role (SecurityConfig)](#step-3--protect-endpoints-by-role-securityconfig)
- [Step 4 — Method-Level Security (@PreAuthorize)](#step-4--method-level-security-preauthorize)
- [Step 5 — Seed an Admin on Startup](#step-5--seed-an-admin-on-startup)
- [Step 6 — Admin-Only Endpoint to Create Other Admins](#step-6--admin-only-endpoint-to-create-other-admins)
- [Step 7 — Testing with Swagger UI](#step-7--testing-with-swagger-ui)
- [Role Design Patterns](#-role-design-patterns)
- [Complete SecurityConfig Example](#-complete-securityconfig-example)
- [Quick Reference — Role Annotations](#-quick-reference--role-annotations)

---

## 🔑 How Roles Work in Spring Security

### The Basics

Every authenticated user has a list of **authorities** (permissions). Spring Security checks these authorities when deciding if a user can access an endpoint.

**Roles are just authorities with a `ROLE_` prefix:**

| Stored in DB | Authority in UserDetails | SecurityConfig check |
|---|---|---|
| `"USER"` | `ROLE_USER` | `.hasRole("USER")` |
| `"ADMIN"` | `ROLE_ADMIN` | `.hasRole("ADMIN")` |
| `"MODERATOR"` | `ROLE_MODERATOR` | `.hasRole("MODERATOR")` |

### The Convention

- **`.hasRole("ADMIN")`** — Automatically prepends `ROLE_` → checks for `ROLE_ADMIN`
- **`.hasAuthority("ROLE_ADMIN")`** — Checks the exact string `ROLE_ADMIN`
- **Both do the same thing**, but `.hasRole()` is cleaner

---

## 🔄 The Role Flow (End to End)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          ROLE FLOW — REGISTRATION TO ACCESS                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  1. REGISTRATION                                                                │
│  ┌────────────┐    POST /api/auth/register     ┌──────────────┐                │
│  │   Client    │ ──────────────────────────────►│ AuthController│                │
│  │             │   { email, password, role }    │              │                │
│  └────────────┘                                 └──────┬───────┘                │
│                                                        │                        │
│  2. SAVE TO DB                                         ▼                        │
│  ┌──────────────┐   User { email, password(hashed), role: "ADMIN" }            │
│  │   Database    │◄────────────────────────────────────┘                        │
│  │ users table   │                                                              │
│  └──────────────┘                                                               │
│                                                                                 │
│  3. GENERATE TOKEN (role is embedded in JWT claims)                             │
│     JWT payload: { "sub": "admin@test.com", "role": "ADMIN", "exp": ... }      │
│                                                                                 │
│  4. LATER — CLIENT ACCESSES PROTECTED ENDPOINT                                  │
│  ┌────────────┐   DELETE /api/students/1        ┌──────────────────────┐        │
│  │   Client    │   Authorization: Bearer eyJ...  │ JwtAuthFilter        │        │
│  │             │ ──────────────────────────────► │                      │        │
│  └────────────┘                                  └──────────┬───────────┘        │
│                                                             │                   │
│  5. FILTER LOADS USER FROM DB                               ▼                   │
│  ┌──────────────────────────┐   loadUserByUsername("admin@test.com")            │
│  │ CustomUserDetailsService  │◄────────────────────────────┘                    │
│  │                          │                                                   │
│  │ Returns UserDetails with:│                                                   │
│  │   authorities: [ROLE_ADMIN]                                                  │
│  └──────────────────────────┘                                                   │
│                                                                                 │
│  6. SPRING SECURITY CHECKS AUTHORIZATION                                        │
│  ┌──────────────────────────────────────────┐                                   │
│  │ SecurityConfig rule:                      │                                  │
│  │   .requestMatchers(DELETE, "/api/**")      │                                  │
│  │   .hasRole("ADMIN")                       │                                  │
│  │                                           │                                  │
│  │ User has: ROLE_ADMIN                      │                                  │
│  │ Required:  ROLE_ADMIN                     │                                  │
│  │ Result:   ✅ ACCESS GRANTED               │                                  │
│  └──────────────────────────────────────────┘                                   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Current State — What You Already Have

Your project already has the foundation for role-based access:

| Component | Status | Details |
|---|---|---|
| `User` entity | ✅ Has `role` field | `@Column(nullable = false, length = 20)` |
| `AuthController` | ✅ Sets role on register | Hardcoded to `"USER"` |
| `JwtService` | ✅ Embeds role in token | `.claim("role", role)` |
| `CustomUserDetailsService` | ⚠️ Bug: wrong prefix | `"Role_"` should be `"ROLE_"` |
| `SecurityConfig` | ⚠️ No role rules yet | Only `permitAll()` and `authenticated()` |

---

## Step 1 — Fix the Role Prefix Bug

**File:** `src/main/java/.../Security/CustomUserDetailsService.java`

```java
// ❌ BEFORE — "Role_USER" doesn't match Spring Security's expected "ROLE_USER"
List.of(new SimpleGrantedAuthority("Role_" + user.getRole()))

// ✅ AFTER — "ROLE_USER" matches .hasRole("USER")
List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
```

**Without this fix, NO role-based rules will work.** `.hasRole("ADMIN")` checks for `ROLE_ADMIN`, but `Role_ADMIN` ≠ `ROLE_ADMIN`.

---

## Step 2 — Register Users with Specific Roles

### Option A — Add Role to Registration Request (Simple)

**Modify `AuthRequest.java`** to accept an optional role:

```java
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

    // Optional — defaults to "USER" if not provided
    private String role;
}
```

**Modify `AuthController.register()`** to use the provided role (with validation):

```java
@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED)
public AuthResponse register(@Valid @RequestBody AuthRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
        throw new ResourceAlreadyExistsException("Email already registered");
    }

    // Determine role — default to "USER", validate allowed values
    String role = (request.getRole() != null) ? request.getRole().toUpperCase() : "USER";

    // Optional: Restrict which roles can be self-registered
    if (!List.of("USER", "MODERATOR").contains(role)) {
        throw new IllegalArgumentException("Cannot self-register as " + role);
    }

    User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .build();

    userRepository.save(user);

    String token = jwtService.generateToken(user.getEmail(), user.getRole());

    return AuthResponse.builder()
            .token(token)
            .email(user.getEmail())
            .role(user.getRole())
            .build();
}
```

**Register a USER:**
```json
POST /api/auth/register
{
  "email": "user@test.com",
  "password": "password123"
}
// role defaults to "USER"
```

**Register a MODERATOR:**
```json
POST /api/auth/register
{
  "email": "mod@test.com",
  "password": "password123",
  "role": "MODERATOR"
}
```

### Option B — Admin-Only Endpoint (Secure)

See [Step 6](#step-6--admin-only-endpoint-to-create-other-admins) for creating an admin-protected endpoint that can assign ANY role.

---

## Step 3 — Protect Endpoints by Role (SecurityConfig)

### URL-Level Rules

In `SecurityConfig.securityFilterChain()`, add role-based rules:

```java
.authorizeHttpRequests(auth -> auth
    // ── Public endpoints (no token needed) ──
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/test").permitAll()
    .requestMatchers("/h2-console/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
    .requestMatchers("/error").permitAll()

    // ── Role-based rules (order matters — specific rules FIRST) ──
    // Only ADMIN can delete anything
    .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

    // Only ADMIN can create teachers
    .requestMatchers(HttpMethod.POST, "/api/teachers").hasRole("ADMIN")

    // ADMIN or MODERATOR can update students
    .requestMatchers(HttpMethod.PUT, "/api/students/**").hasAnyRole("ADMIN", "MODERATOR")

    // Any authenticated user can read
    .requestMatchers(HttpMethod.GET, "/api/**").authenticated()

    // Everything else requires authentication
    .anyRequest().authenticated()
)
```

### Rule Order Matters!

Spring Security evaluates rules **top to bottom** and uses the **first match**:

```java
// ✅ CORRECT — specific rule first, then general
.requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")   // matched first for DELETE
.requestMatchers("/api/**").authenticated()                        // matched for everything else

// ❌ WRONG — general rule catches everything, specific rule never reached
.requestMatchers("/api/**").authenticated()                        // matches ALL /api/** requests
.requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")   // never reached!
```

### Available Role Check Methods

| Method | Meaning | Example |
|---|---|---|
| `.hasRole("ADMIN")` | User must have `ROLE_ADMIN` | Admin-only endpoints |
| `.hasAnyRole("ADMIN", "MOD")` | User must have `ROLE_ADMIN` OR `ROLE_MOD` | Multiple roles allowed |
| `.hasAuthority("ROLE_ADMIN")` | Exact authority match | Same as `hasRole("ADMIN")` |
| `.authenticated()` | Any logged-in user (any role) | General protected endpoints |
| `.permitAll()` | No authentication needed | Public endpoints |
| `.denyAll()` | Block everyone | Disabled endpoints |

---

## Step 4 — Method-Level Security (@PreAuthorize)

For more fine-grained control, you can put security rules directly on controller or service methods.

### Enable Method Security

Add `@EnableMethodSecurity` to your `SecurityConfig`:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity        // ← ADD THIS
@RequiredArgsConstructor
public class SecurityConfig { ... }
```

### Use @PreAuthorize on Controllers

```java
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // Anyone authenticated can read
    @GetMapping
    public List<StudentResponseDto> getAllStudents() {
        return studentService.getAllStudents();
    }

    // Only ADMIN can delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
    }

    // ADMIN or MODERATOR can update
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public StudentResponseDto updateStudent(@PathVariable Long id,
                                            @RequestBody StudentRequestDto dto) {
        return studentService.updateStudent(id, dto);
    }

    // Only the user themselves or ADMIN can view details
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public StudentResponseDto getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }
}
```

### @PreAuthorize vs SecurityConfig Rules — When to Use Which?

| Approach | Best for | Example |
|---|---|---|
| **SecurityConfig** (URL rules) | Broad rules by HTTP method + path | "All DELETEs require ADMIN" |
| **@PreAuthorize** (method-level) | Fine-grained, per-method rules | "Only ADMIN or the owner can view" |
| **Both together** | Defense in depth | SecurityConfig as a safety net, @PreAuthorize for specific logic |

### Available @PreAuthorize Expressions

```java
@PreAuthorize("hasRole('ADMIN')")                          // Single role
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")          // Multiple roles
@PreAuthorize("hasAuthority('ROLE_ADMIN')")                // Exact authority
@PreAuthorize("isAuthenticated()")                          // Any logged-in user
@PreAuthorize("permitAll()")                                // Everyone (rarely used on methods)
@PreAuthorize("#email == authentication.name")              // Current user's email matches param
@PreAuthorize("hasRole('ADMIN') or #id == principal.userId") // ADMIN or resource owner
```

---

## Step 5 — Seed an Admin on Startup

Use `CommandLineRunner` to create a default admin when the app starts (if one doesn't already exist):

**Create** `src/main/java/.../Security/AdminSeeder.java`:

```java
package com.codeWithJeff.SampleSpringBootApplication.Security;

import com.codeWithJeff.SampleSpringBootApplication.Entity.User;
import com.codeWithJeff.SampleSpringBootApplication.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default ADMIN user on application startup.
 * Only creates the admin if no admin exists yet.
 *
 * WHY: You need at least one ADMIN to create other admins.
 * This avoids the "chicken and egg" problem.
 *
 * IMPORTANT: Change the default password in production!
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@admin.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .build();

            userRepository.save(admin);
            log.info("✅ Default ADMIN user created: {}", adminEmail);
        } else {
            log.info("ℹ️ ADMIN user already exists: {}", adminEmail);
        }
    }
}
```

**On startup, the console shows:**
```
✅ Default ADMIN user created: admin@admin.com
```

**Login as admin:**
```json
POST /api/auth/login
{
  "email": "admin@admin.com",
  "password": "admin123"
}
// Response: { "token": "eyJhb...", "email": "admin@admin.com", "role": "ADMIN" }
```

---

## Step 6 — Admin-Only Endpoint to Create Other Admins

Add a protected endpoint in `AuthController` that only existing ADMINs can use:

```java
/**
 * Admin-only: Create a user with ANY role.
 * Requires: Valid JWT token from an ADMIN user.
 *
 * POST /api/auth/admin/create-user
 * Authorization: Bearer <admin-token>
 * Body: { "email": "newadmin@test.com", "password": "pass123", "role": "ADMIN" }
 */
@PostMapping("/admin/create-user")
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("hasRole('ADMIN')")
public AuthResponse createUserAsAdmin(@Valid @RequestBody AuthRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
        throw new ResourceAlreadyExistsException("Email already registered");
    }

    // Admin can assign any role
    String role = (request.getRole() != null) ? request.getRole().toUpperCase() : "USER";

    User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .build();

    userRepository.save(user);

    String token = jwtService.generateToken(user.getEmail(), user.getRole());

    return AuthResponse.builder()
            .token(token)
            .email(user.getEmail())
            .role(user.getRole())
            .build();
}
```

**Note:** This endpoint is under `/api/auth/**` which is `permitAll()` in SecurityConfig. The `@PreAuthorize("hasRole('ADMIN')")` provides the protection at the method level. Alternatively, you could move it to a different path like `/api/admin/create-user` and protect it in SecurityConfig.

---

## Step 7 — Testing with Swagger UI

### 7.1 — Open Swagger UI
Navigate to: `http://localhost:7000/swagger-ui/index.html`

### 7.2 — Register a Regular User
1. Find `POST /api/auth/register`
2. Click **Try it out**
3. Enter:
   ```json
   {
     "email": "user@test.com",
     "password": "password123"
   }
   ```
4. Click **Execute**
5. Copy the `token` from the response

### 7.3 — Authenticate in Swagger
1. Click the **🔒 Authorize** button (top right of Swagger UI)
2. In the **Value** field, enter: `Bearer <paste-your-token-here>`
3. Click **Authorize** → Close

### 7.4 — Test Protected Endpoints
Now all requests from Swagger include the JWT token automatically:
- `GET /api/students` → 200 ✅ (authenticated)
- `DELETE /api/students/1` → 403 ❌ (requires ADMIN, you're USER)

### 7.5 — Login as Admin
1. Find `POST /api/auth/login`
2. Enter:
   ```json
   {
     "email": "admin@admin.com",
     "password": "admin123"
   }
   ```
3. Copy the new token
4. Click **Authorize** again → paste the admin token
5. `DELETE /api/students/1` → 204 ✅ (you're ADMIN now)

### 7.6 — Add OpenAPI Security Scheme (Optional)

To make the Authorize button appear automatically in Swagger, add this config:

**Create** `src/main/java/.../Security/OpenApiConfig.java`:

```java
package com.codeWithJeff.SampleSpringBootApplication.Security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class OpenApiConfig {
}
```

Then annotate your controllers:
```java
@RestController
@RequestMapping("/api/students")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class StudentController { ... }
```

---

## 🏗️ Role Design Patterns

### Pattern 1 — Simple Roles (Your Current Setup)

Store a single role string in the `User` entity:

```java
@Column(nullable = false, length = 20)
private String role;   // "USER", "ADMIN", "MODERATOR"
```

**Pros:** Simple, easy to understand.
**Cons:** User can only have ONE role.
**Best for:** Small apps, MVPs, learning projects.

### Pattern 2 — Multiple Roles (Enum Set)

Store roles as a comma-separated string or a Set:

```java
// Option A: Comma-separated string
@Column(nullable = false)
private String roles;   // "USER,MODERATOR"

// Option B: ElementCollection (separate table)
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
@Column(name = "role")
private Set<String> roles = new HashSet<>();
```

Then in `CustomUserDetailsService`:
```java
// For comma-separated:
List<SimpleGrantedAuthority> authorities = Arrays.stream(user.getRoles().split(","))
    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
    .toList();

// For ElementCollection:
List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
    .toList();
```

### Pattern 3 — Role Entity (Full Flexibility)

Create a separate `Role` entity with a many-to-many relationship:

```java
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;   // "ADMIN", "USER", "MODERATOR"
}

@Entity
@Table(name = "users")
public class User {
    // ...other fields...

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
```

**Pros:** Most flexible, supports permissions within roles.
**Cons:** More complex, more tables, more code.
**Best for:** Enterprise applications with complex authorization needs.

---

## 📋 Complete SecurityConfig Example

Here's what a fully configured `SecurityConfig` looks like with role-based rules:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity                   // Enables @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))

            .authorizeHttpRequests(auth -> auth
                // ── Public (no token needed) ──
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/test").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/error").permitAll()

                // ── ADMIN only ──
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/teachers").hasRole("ADMIN")

                // ── ADMIN or MODERATOR ──
                .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("ADMIN", "MODERATOR")

                // ── Any authenticated user ──
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/**").authenticated()

                // ── Catch-all ──
                .anyRequest().authenticated()
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## 📌 Quick Reference — Role Annotations

### SecurityConfig (URL-level)

```java
.requestMatchers("/path").permitAll()                    // No auth needed
.requestMatchers("/path").authenticated()                 // Any logged-in user
.requestMatchers("/path").hasRole("ADMIN")                // ROLE_ADMIN only
.requestMatchers("/path").hasAnyRole("ADMIN", "MOD")     // ROLE_ADMIN or ROLE_MOD
.requestMatchers(HttpMethod.DELETE, "/path").hasRole("X") // HTTP method + role
.requestMatchers("/path").denyAll()                       // Block everyone
```

### Method-level (@PreAuthorize)

```java
@PreAuthorize("hasRole('ADMIN')")                           // ADMIN only
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")           // ADMIN or MODERATOR
@PreAuthorize("isAuthenticated()")                           // Any logged-in user
@PreAuthorize("#email == authentication.name")               // Current user only
@PreAuthorize("hasRole('ADMIN') or #email == authentication.name")  // ADMIN or owner
```

### Required Setup

```java
// SecurityConfig class must have:
@EnableMethodSecurity          // Enables @PreAuthorize

// CustomUserDetailsService must use:
"ROLE_" + user.getRole()       // UPPERCASE prefix — NOT "Role_"

// JwtService must embed role in token:
.claim("role", role)           // So the token carries role info
```

---

**Document Version:** 1.0
**Created:** April 8, 2026
**Prerequisites:** JWT Authentication Guide (docs/JWT_AUTHENTICATION_GUIDE.md), Troubleshooting Guide (docs/TROUBLESHOOTING_GUIDE.md)

