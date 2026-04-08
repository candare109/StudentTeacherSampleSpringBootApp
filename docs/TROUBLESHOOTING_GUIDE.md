# 🔧 Troubleshooting Guide — All Bugs Fixed & How to Diagnose Errors

**Last Updated:** April 8, 2026

---

## 📑 Table of Contents

- [All Changes Made (Bug Fixes)](#-all-changes-made-bug-fixes)
- [How to Read bootRun Errors](#-how-to-read-bootrun-errors)
- [Error Diagnosis Flowchart](#-error-diagnosis-flowchart)
- [Common Spring Boot Startup Errors](#-common-spring-boot-startup-errors)
- [Common Runtime Errors (App Running but Endpoints Fail)](#-common-runtime-errors)
- [PostgreSQL-Specific Issues](#-postgresql-specific-issues)
- [YAML Configuration Pitfalls](#-yaml-configuration-pitfalls)
- [Spring Security Error Patterns](#-spring-security-error-patterns)

---

## ✅ All Changes Made (Bug Fixes)

Four bugs were discovered and fixed on **April 8, 2026**. All four are connected — the first three caused the fourth.

### Bug #1 — YAML Flow Mapping (`{DB-JWT-SECRET}` without `$`)

| | |
|---|---|
| **File** | `src/main/resources/application-postgres.yml` line 34 |
| **Symptom** | `Error creating bean with name 'jwtService': Injection of autowired dependencies failed` |
| **Root Cause** | In YAML, curly braces `{...}` are **flow mapping syntax** (an inline object). Writing `secret: {DB-JWT-SECRET}` makes YAML parse it as a `Map`, not a `String`. Spring then can't inject a Map into `JwtService.secret` (a `String` field). |
| **Fix** | Add `$` prefix and **quote** the value so YAML treats it as a string |

```yaml
# ❌ BEFORE — YAML parses {DB-JWT-SECRET} as a Map: {DB-JWT-SECRET: null}
jwt:
  secret: {DB-JWT-SECRET}

# ✅ AFTER — Quoted string, Spring resolves ${DB-JWT-SECRET} as a property placeholder
jwt:
  secret: "${DB-JWT-SECRET}"
```

**YAML value parsing cheat sheet:**

| YAML value | Parsed as | Type |
|---|---|---|
| `secret: hello` | `"hello"` | ✅ String |
| `secret: "hello"` | `"hello"` | ✅ String (explicitly quoted) |
| `secret: "${DB-JWT-SECRET}"` | Spring resolves the placeholder | ✅ String |
| `secret: {DB-JWT-SECRET}` | `{DB-JWT-SECRET: null}` | ❌ **Map** (YAML flow mapping!) |
| `secret: [a, b, c]` | `["a", "b", "c"]` | ❌ **List** (YAML flow sequence!) |

**Rule:** Always **quote** values that contain `${}`, `{}`, `[]`, `:`, `#`, or special characters.

---

### Bug #2 — Reserved Keyword Table Name (`user` in PostgreSQL)

| | |
|---|---|
| **File** | `src/main/java/.../Entity/User.java` line 11 |
| **Symptom** | SQL syntax error when Hibernate tries to `INSERT INTO user ...` or `CREATE TABLE user ...` |
| **Root Cause** | `user` is a **reserved keyword** in PostgreSQL (it's a built-in function). Hibernate generates unquoted SQL, and PostgreSQL rejects it. |
| **Fix** | Rename the table to `users` (plural) |

```java
// ❌ BEFORE — "user" is a reserved keyword in PostgreSQL
@Table(name = "user")

// ✅ AFTER — "users" is safe
@Table(name = "users")
```

**PostgreSQL reserved keywords to avoid as table names:**

| Keyword | Why |
|---|---|
| `user` | Built-in function — `SELECT user` returns the current user |
| `order` | SQL clause — `ORDER BY` |
| `table` | SQL keyword — `CREATE TABLE` |
| `group` | SQL clause — `GROUP BY` |
| `select`, `insert`, `update`, `delete` | SQL commands |
| `index` | SQL keyword — `CREATE INDEX` |
| `key` | SQL keyword |
| `check` | SQL constraint keyword |
| `references` | SQL FK keyword |

**Rule:** Always use **plural nouns** for table names (`students`, `teachers`, `users`, `orders`). This avoids reserved keyword collisions and is the standard JPA convention.

---

### Bug #3 — Missing `@NoArgsConstructor` + `@AllArgsConstructor` on Entity

| | |
|---|---|
| **File** | `src/main/java/.../Entity/User.java` |
| **Symptom** | `org.hibernate.InstantiationException: No default constructor for entity` or JPA/Hibernate fails to instantiate the entity when loading from database |
| **Root Cause** | `@Builder` generates a **package-private all-args constructor** which **suppresses** Java's default no-arg constructor. JPA/Hibernate **requires** a no-arg constructor to instantiate entities when reading from the database. |
| **Fix** | Add both `@NoArgsConstructor` and `@AllArgsConstructor` |

```java
// ❌ BEFORE — @Builder suppresses default constructor, JPA can't instantiate
@Entity
@Table(name = "users")
@Data
@Builder
public class User { ... }

// ✅ AFTER — Explicit constructors for both JPA and @Builder
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor      // ← JPA needs this
@AllArgsConstructor     // ← @Builder needs this
public class User { ... }
```

**Why both?**

| Annotation | Why needed |
|---|---|
| `@NoArgsConstructor` | **JPA/Hibernate requires it.** When Hibernate loads an entity from the DB, it creates an empty instance using the no-arg constructor, then sets fields via reflection. |
| `@AllArgsConstructor` | **`@Builder` requires it.** The Builder pattern calls `new User(field1, field2, ...)` internally. Without this, `@Builder` generates its own package-private constructor, which suppresses the no-arg one. |

**Rule:** Every JPA `@Entity` that uses `@Builder` must have BOTH `@NoArgsConstructor` AND `@AllArgsConstructor`. Use this standard 5-annotation combo:

```java
@Entity
@Table(name = "table_name")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YourEntity { ... }
```

---

### Bug #4 — `/error` Endpoint Not Permitted in SecurityConfig

| | |
|---|---|
| **File** | `src/main/java/.../Security/SecurityConfig.java` line 76 |
| **Symptom** | `403 Forbidden` with **empty body** (`content-length: 0`) on any endpoint that throws an exception |
| **Root Cause** | When a controller throws an exception, the servlet container forwards the request to Spring Boot's `/error` endpoint. This forward goes through the Spring Security filter chain again. Since `/error` was NOT in the `permitAll()` list, it matched `.anyRequest().authenticated()`. With no authentication and no `formLogin`/`httpBasic` configured, Spring Security's default `Http403ForbiddenEntryPoint` returned **403 with an empty body** — completely masking the real error. |
| **Fix** | Add `/error` to the `permitAll()` list |

```java
// ❌ BEFORE — /error not permitted, real errors get masked as 403
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
    .anyRequest().authenticated()
)

// ✅ AFTER — /error permitted, real error messages are visible
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
    .requestMatchers("/error").permitAll()       // ← ADD THIS
    .anyRequest().authenticated()
)
```

**How the 403 masking worked (before the fix):**

```
1. POST /api/auth/register → permitAll ✅ → enters controller
2. Controller throws exception (e.g., SQL error from reserved keyword "user")
3. Servlet container forwards to → GET /error (Spring Boot's error handler)
4. Spring Security evaluates /error → .anyRequest().authenticated() → BLOCKED
5. No formLogin/httpBasic configured → Http403ForbiddenEntryPoint
6. Client sees: 403 Forbidden, empty body  ← REAL ERROR IS HIDDEN
```

**After the fix:**

```
1. POST /api/auth/register → permitAll ✅ → enters controller
2. Controller throws exception (e.g., SQL error)
3. Servlet container forwards to → GET /error → permitAll ✅
4. Spring Boot's BasicErrorController returns the REAL error:
   { "status": 500, "error": "Internal Server Error", "message": "..." }
```

**Rule:** ALWAYS add `.requestMatchers("/error").permitAll()` in any Spring Security config. Without it, every unhandled exception becomes a mysterious 403.

---

### Bug #5 — Wrong Role Prefix in CustomUserDetailsService

| | |
|---|---|
| **File** | `src/main/java/.../Security/CustomUserDetailsService.java` line 40 |
| **Symptom** | `.hasRole("ADMIN")` in SecurityConfig never matches — users with ADMIN role still get 403 on role-protected endpoints |
| **Root Cause** | Spring Security's `.hasRole("ADMIN")` checks for the authority `ROLE_ADMIN` (uppercase prefix). The code used `"Role_"` (lowercase 'o', lowercase 'l') instead of `"ROLE_"` (all uppercase). |
| **Fix** | Change `"Role_"` to `"ROLE_"` |

```java
// ❌ BEFORE — "Role_USER" doesn't match Spring Security's expected "ROLE_USER"
List.of(new SimpleGrantedAuthority("Role_" + user.getRole()))

// ✅ AFTER — "ROLE_USER" matches .hasRole("USER")
List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
```

**How Spring Security role matching works:**

| Method in SecurityConfig | What it checks | Authority needed in UserDetails |
|---|---|---|
| `.hasRole("ADMIN")` | Adds `ROLE_` prefix automatically | `ROLE_ADMIN` |
| `.hasRole("USER")` | Adds `ROLE_` prefix automatically | `ROLE_USER` |
| `.hasAuthority("ROLE_ADMIN")` | Checks exact string | `ROLE_ADMIN` |
| `.hasAuthority("custom_perm")` | Checks exact string | `custom_perm` |

**Rule:** When using `.hasRole("X")`, the authority in `UserDetails` must be `ROLE_X` (uppercase). Use `.hasAuthority()` if you want to skip the `ROLE_` prefix convention.

---

## 🔍 How to Read bootRun Errors

When you run `.\gradlew.bat bootRun` and the app fails, the console output contains the answer. Here's how to read it:

### Step 1 — Find the Root Cause

Scroll to the **bottom** of the error stack trace. Look for `Caused by:` — the **last** `Caused by:` is the root cause:

```
ERROR o.s.boot.SpringApplication - Application run failed
org.springframework.beans.factory.BeanCreationException:
  Error creating bean with name 'securityConfig': ...
    Caused by: org.springframework.beans.factory.BeanCreationException:
      Error creating bean with name 'jwtAuthenticationFilter': ...
        Caused by: org.springframework.beans.factory.BeanCreationException:
          Error creating bean with name 'jwtService': ...
            Caused by: java.lang.IllegalArgumentException:     ← ROOT CAUSE
              Could not resolve placeholder 'jwt.secret'       ← THE ACTUAL ERROR
```

**Read it bottom-up:** The root cause is `Could not resolve placeholder 'jwt.secret'` → the `jwt.secret` property is missing or can't be resolved.

### Step 2 — Identify the Error Category

| Error message contains | Category | Where to look |
|---|---|---|
| `Could not resolve placeholder` | Missing property | `application.yml` / `application-{profile}.yml` |
| `Error creating bean with name` | Bean creation failure | The bean's class — check constructors, `@Value`, dependencies |
| `Injection of autowired dependencies failed` | Dependency injection | The `@Autowired` / `final` field's type — is the dependency bean also failing? |
| `No qualifying bean of type` | Missing bean | The dependency isn't annotated with `@Service`/`@Component`/`@Repository` |
| `Table "xxx" not found` | Database/JPA | Entity `@Table` name, `ddl-auto` setting, profile config |
| `Syntax error in SQL statement` | Reserved keyword / bad SQL | Check `@Table` and `@Column` names against reserved keywords |
| `No default constructor for entity` | Missing no-arg constructor | Add `@NoArgsConstructor` to the entity |
| `Port already in use` | Port conflict | Another process is using port 7000 |

### Step 3 — Check the Bean Dependency Chain

Bean creation errors cascade. If `JwtService` fails, everything that depends on it also fails:

```
JwtService fails (root cause: property not found)
  → JwtAuthenticationFilter fails (depends on JwtService)
    → SecurityConfig fails (depends on JwtAuthenticationFilter)
      → App fails to start

FIX THE DEEPEST ONE (JwtService), and the rest resolve automatically.
```

---

## 📊 Error Diagnosis Flowchart

```
App won't start?
├── Check console for "Caused by:" (last one = root cause)
│
├── "Could not resolve placeholder 'xxx'"
│   ├── Is the property defined in application-{profile}.yml?
│   ├── Is the correct profile active? (check spring.profiles.active)
│   ├── Is the YAML syntax correct? (no unquoted {}, [], :)
│   └── If using Key Vault: is Azure CLI authenticated? (az login)
│
├── "Error creating bean with name 'xxx'"
│   ├── Check the bean's @Value annotations → are properties defined?
│   ├── Check the bean's constructor → are all dependencies available?
│   ├── Check the bean's dependencies → are THEY failing first?
│   └── Read the FULL chain — fix the deepest "Caused by:" first
│
├── "No default constructor for entity: xxx"
│   └── Add @NoArgsConstructor to the entity class
│
├── "Syntax error in SQL statement" / "Table not found"
│   ├── Check @Table(name = "xxx") → is "xxx" a reserved keyword?
│   ├── Check ddl-auto setting → is it "update" or "create-drop"?
│   └── Check the active database profile (H2 vs PostgreSQL)
│
└── "Port already in use: 7000"
    ├── Kill the other process: Get-Process -Id (Get-NetTCPConnection -LocalPort 7000).OwningProcess | Stop-Process
    └── Or change port in application.yml: server.port: 7001


App starts but endpoints return errors?
├── 403 Forbidden (empty body)
│   ├── Is /error in permitAll()? → Add .requestMatchers("/error").permitAll()
│   ├── Is the endpoint in permitAll()? → Check SecurityConfig rules
│   ├── Is CSRF disabled? → Check .csrf(csrf -> csrf.disable())
│   └── Is the real error being masked? → Check server console logs
│
├── 401 Unauthorized
│   ├── Is the Authorization header present? → "Bearer <token>"
│   ├── Is the token expired? → Check jwt.expiration value
│   ├── Is the token signed with the correct secret?
│   └── Is the endpoint in permitAll()? → Maybe it requires auth
│
├── 500 Internal Server Error
│   ├── Check console for the full stack trace
│   ├── Is it a database error? → Check DB connection, table names
│   ├── Is it a null pointer? → Check entity relationships, Optional handling
│   └── Is it a WeakKeyException? → JWT secret must be ≥ 32 characters
│
└── 404 Not Found
    ├── Is the URL correct? (/api/students vs /api/student)
    ├── Is the controller annotated with @RestController?
    ├── Is @RequestMapping correct?
    └── Is the resource actually in the database?
```

---

## 💡 Common Spring Boot Startup Errors

### 1. Missing Property Placeholder

```
Could not resolve placeholder 'jwt.secret' in value "${jwt.secret}"
```

**Cause:** The property isn't defined in the active profile's YAML file.

**Fix checklist:**
1. Check which profile is active: `spring.profiles.active` in `application.yml`
2. Open the matching `application-{profile}.yml`
3. Ensure the property exists under the correct YAML hierarchy
4. If using environment variables: ensure they're set (`echo $env:JWT_SECRET`)
5. If using Azure Key Vault: ensure `az login` is authenticated

### 2. Circular Dependency

```
The dependencies of some of the beans in the application context form a cycle
```

**Cause:** Bean A depends on Bean B, and Bean B depends on Bean A.

**Fix:** Break the cycle by:
- Using `@Lazy` on one of the dependencies
- Restructuring so one bean doesn't need the other
- Using setter injection (`@Autowired` on a setter method) instead of constructor injection for one side

### 3. Multiple Beans of Same Type

```
No qualifying bean of type 'xxx': expected single matching bean but found 2
```

**Cause:** Two `@Service` or `@Component` classes implement the same interface.

**Fix:** Use `@Primary` on the preferred one, or `@Qualifier("beanName")` at the injection point.

### 4. JPA Entity Validation Fails at Startup

```
Schema-validation: missing table [users]
```

**Cause:** `ddl-auto: validate` expects the table to already exist.

**Fix:** Use `ddl-auto: update` (creates/modifies tables) or `ddl-auto: create-drop` (recreates on every startup, good for H2 dev).

### 5. Lombok Not Generating Code

```
cannot find symbol: method getEmail()
```

**Cause:** Lombok annotation processor not configured.

**Fix:** Ensure `build.gradle` has:
```groovy
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
```
And in IntelliJ: Settings → Build → Compiler → Annotation Processors → ✅ Enable.

---

## 🐘 PostgreSQL-Specific Issues

### Reserved Keywords

PostgreSQL has more reserved keywords than H2. Code that works on H2 may fail on PostgreSQL.

**Quick test:** If your app works on the `h2` profile but fails on `postgres`, check your `@Table` and `@Column` names.

### Case Sensitivity

PostgreSQL folds unquoted identifiers to **lowercase**:
- `@Column(name = "firstName")` → PostgreSQL creates `firstname`
- `@Column(name = "FIRSTNAME")` → PostgreSQL creates `firstname`

If you need mixed case, quote the name: `@Column(name = "\"firstName\"")` — but this is **not recommended**. Use snake_case instead: `first_name`.

### Connection Refused

```
Connection to localhost:5432 refused
```

**Checklist:**
1. Is PostgreSQL running? (`docker ps` or check Azure portal)
2. Are `DB-URL`, `DB-USERNAME`, `DB-PASSWORD` set? (environment variables or Key Vault)
3. Does the database `studentdb` exist?
4. Is the firewall allowing your IP? (Azure Portal → Networking)

---

## 📝 YAML Configuration Pitfalls

### Pitfall 1 — Unquoted Special Characters

```yaml
# ❌ BROKEN — YAML interprets these as special syntax
password: p@ss:word       # ":" splits into key:value
secret: {MY_SECRET}       # "{}" is a flow mapping
list: [a, b, c]           # "[]" is a flow sequence
comment: hello # world    # "#" starts a comment

# ✅ FIXED — Quote values with special characters
password: "p@ss:word"
secret: "${MY_SECRET}"
list: "[a, b, c]"         # if you want a literal string
comment: "hello # world"
```

### Pitfall 2 — Indentation Matters

YAML uses **spaces only** (not tabs). Wrong indentation = wrong property hierarchy:

```yaml
# ❌ WRONG — jwt is nested under server (indentation error)
server:
  port: 7000
  jwt:
    secret: "my-secret"   # This becomes server.jwt.secret, NOT jwt.secret

# ✅ CORRECT — jwt is at the root level
server:
  port: 7000

jwt:
  secret: "my-secret"     # This is jwt.secret ✓
```

### Pitfall 3 — Profile-Specific Properties

Properties in `application.yml` are **always loaded**. Profile-specific files **override** them:

```
application.yml                  → Always loaded (base config)
application-h2.yml               → Loaded when profile = h2
application-postgres.yml         → Loaded when profile = postgres
```

If `jwt.secret` is defined in `application-h2.yml` but NOT in `application-postgres.yml`, switching to the postgres profile means `jwt.secret` doesn't exist → bean creation failure.

---

## 🔒 Spring Security Error Patterns

### Pattern: 403 with Empty Body

```
Status: 403
Headers: content-length: 0, x-content-type-options: nosniff
Body: (empty)
```

**This is ALWAYS Spring Security blocking the request.** Causes:
1. `/error` not in `permitAll()` → real error is masked (most common!)
2. CSRF protection enabled → POST/PUT/DELETE blocked without CSRF token
3. Endpoint not in `permitAll()` → requires authentication
4. Role mismatch → user has `ROLE_USER` but endpoint requires `ROLE_ADMIN`

### Pattern: 401 Unauthorized

**Means:** "I don't know who you are." The request has no valid authentication.

**Check:** Is the `Authorization: Bearer <token>` header present and correct?

### Pattern: 403 Forbidden (with body)

**Means:** "I know who you are, but you don't have permission."

**Check:** The user's role doesn't match the endpoint's requirement (e.g., `ROLE_USER` trying to access a `ROLE_ADMIN` endpoint).

### Quick Security Debug Checklist

```
□ Is CSRF disabled?                    → .csrf(csrf -> csrf.disable())
□ Is /error in permitAll()?            → .requestMatchers("/error").permitAll()
□ Is the endpoint path correct?        → /api/auth/** vs /api/auth/register
□ Is the role prefix correct?          → "ROLE_ADMIN" not "Role_ADMIN"
□ Is the JWT token valid?              → Check expiry, signature, claims
□ Is the signing key ≥ 32 characters?  → Required for HS256
□ Is the Authorization header format correct? → "Bearer " + token (with space)
```

---

**Document Version:** 1.0
**Created:** April 8, 2026
**Context:** Fixes applied to resolve bean creation errors and 403 Forbidden responses during JWT authentication implementation.

