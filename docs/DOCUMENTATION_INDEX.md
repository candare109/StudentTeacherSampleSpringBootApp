# Documentation Index

**Last Updated:** April 8, 2026

## Available Documentation

### Getting Started
- **`README.md`**
  - Quick start, endpoints, profiles, Azure run examples, psql commands, git guide, and troubleshooting.

### Architecture & Patterns
- **`docs/COMPLETE_PROJECT_GUIDE.md`**
  - Full project structure, layered architecture, DTO/entity/repository/service/controller relationships, entity relationships (@ManyToOne, @OneToOne), and endpoint reference.

- **`docs/REQUEST_FLOW_GUIDE.md`**
  - Step-by-step request flow using `GET /api/students/{id}` and `POST /api/subject`.

- **`docs/DEVELOPMENT_PATTERNS_GUIDE.md`** ← NEW (April 8, 2026)
  - Complete guide for building new features: Entity → DTO → Repository → Service → Implementation → Controller patterns. Repository query naming conventions. Entity relationships (@ManyToOne, @OneToOne, @ManyToMany). Validation patterns. **Complete annotations reference** (Web, Bean, JPA, Lombok, Security, Eureka, Swagger). New feature checklist.

### Security & Authentication
- **`docs/JWT_AUTHENTICATION_GUIDE.md`**
  - Step-by-step JWT authentication guide — how JWT works, full implementation code, testing with Postman, and request flow diagrams.

- **`docs/ROLE_BASED_ACCESS_GUIDE.md`** ← NEW (April 8, 2026)
  - Role-based access control (RBAC): registering users with roles (USER, ADMIN, MODERATOR), SecurityConfig URL-level rules (`.hasRole()`, `.hasAnyRole()`), method-level security (`@PreAuthorize`), seeding a default admin with `CommandLineRunner`, admin-only endpoint to create other admins, testing roles with Swagger UI, OpenAPI security scheme configuration.

### Infrastructure & Cloud
- **`docs/AZURE_POSTGRES_SETUP.md`**
  - Azure Database for PostgreSQL Flexible Server integration steps, environment variables, psql commands, and troubleshooting.

- **`docs/AZURE_KEYVAULT_SETUP.md`**
  - Step-by-step Azure Key Vault creation (every portal tab explained), storing secrets, granting access, Spring Boot integration, and troubleshooting.

- **`docs/EUREKA_SERVICE_DISCOVERY_GUIDE.md`** ← NEW (April 8, 2026)
  - Netflix Eureka service discovery: creating a Eureka Server (separate project), registering this app as a client, inter-service communication (RestClient, WebClient, OpenFeign), API Gateway with Spring Cloud Gateway, health checks, production HA setup, troubleshooting.

### Troubleshooting
- **`docs/TROUBLESHOOTING_GUIDE.md`** ← NEW (April 8, 2026)
  - All bugs fixed on April 8, 2026 (YAML flow mapping, reserved keyword table name, missing constructors, `/error` not permitted, wrong role prefix). How to read `bootRun` errors. Error diagnosis flowchart. Common Spring Boot startup errors. PostgreSQL-specific issues. YAML configuration pitfalls. Spring Security error patterns (403 vs 401 debugging).

## Suggested Reading Order

### If you're new to the project:
1. `README.md`
2. `docs/COMPLETE_PROJECT_GUIDE.md`
3. `docs/REQUEST_FLOW_GUIDE.md`
4. `docs/DEVELOPMENT_PATTERNS_GUIDE.md`

### If you're adding authentication:
5. `docs/JWT_AUTHENTICATION_GUIDE.md`
6. `docs/ROLE_BASED_ACCESS_GUIDE.md`

### If you're deploying to Azure:
7. `docs/AZURE_POSTGRES_SETUP.md`
8. `docs/AZURE_KEYVAULT_SETUP.md`

### If you're building microservices:
9. `docs/EUREKA_SERVICE_DISCOVERY_GUIDE.md`

### If something breaks:
10. `docs/TROUBLESHOOTING_GUIDE.md`

## Current Project State (April 8, 2026)

### Entities
| Entity | Table | Fields |
|---|---|---|
| Student | `students` | studentId, firstName, lastName, email, age, course |
| Teacher | `teachers` | teacherId, firstName, lastName |
| Subject | `subject` | subjectId, student (FK), teacher (FK), subject |
| User | `users` | userId, email, password, role |
| Grades | `grades` | *(see entity)* |
| ClassAssignment | *(see entity)* | *(see entity)* |

### Entity Relationships
| Relationship | Type | Constraint |
|---|---|---|
| Subject → Student | `@ManyToOne` | Many subjects can reference one student |
| Subject → Teacher | `@OneToOne` | Each teacher teaches exactly one subject |

### API Endpoints

| Resource | Base URL | Operations | Auth Required |
|---|---|---|---|
| Auth | `/api/auth` | POST register, POST login | No (public) |
| Students | `/api/students` | POST, GET all, GET by ID, PUT, DELETE | Yes (JWT) |
| Teachers | `/api/teachers` | POST, GET all, GET by ID, DELETE | Yes (JWT) |
| Subjects | `/api/subject` | POST | Yes (JWT) |
| Grades | `/api/grades` | *(see controller)* | Yes (JWT) |
| Class Assignments | `/api/class-assignments` | *(see controller)* | Yes (JWT) |

### Security Configuration
| Endpoint Pattern | Access Level |
|---|---|
| `/api/auth/**` | Public (permitAll) |
| `/test` | Public (permitAll) |
| `/h2-console/**` | Public (permitAll) |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public (permitAll) |
| `/error` | Public (permitAll) |
| Everything else | Authenticated (JWT required) |

## Bugs Fixed (April 8, 2026)

| # | Bug | File | Fix |
|---|---|---|---|
| 1 | YAML `{DB-JWT-SECRET}` parsed as Map | `application-postgres.yml` | Changed to `"${DB-JWT-SECRET}"` |
| 2 | `@Table(name = "user")` — reserved keyword | `Entity/User.java` | Changed to `"users"` |
| 3 | Missing `@NoArgsConstructor` + `@AllArgsConstructor` | `Entity/User.java` | Added both annotations |
| 4 | `/error` not in `permitAll()` — 403 masking | `SecurityConfig.java` | Added `.requestMatchers("/error").permitAll()` |
| 5 | `"Role_"` prefix instead of `"ROLE_"` | `CustomUserDetailsService.java` | Changed to `"ROLE_"` |

## Running the Application

```powershell
# Local H2 (quick dev)
$env:SPRING_PROFILES_ACTIVE="h2"; .\gradlew.bat bootRun

# Azure PostgreSQL (with Key Vault)
$env:SPRING_PROFILES_ACTIVE="postgres"; .\gradlew.bat bootRun

# Swagger UI: http://localhost:7000/swagger-ui/index.html
# H2 Console:  http://localhost:7000/h2-console
```
