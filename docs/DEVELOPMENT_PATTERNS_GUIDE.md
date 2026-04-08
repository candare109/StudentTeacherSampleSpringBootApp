# 🏗️ Development Patterns Guide — Entity, DTO, Service, Implementation & Annotations

**Last Updated:** April 8, 2026

A complete guide for building new features in your Spring Boot project. Follow this as your blueprint so you can build confidently without relying on prompts. Every pattern, annotation, and decision is explained with *why*.

---

## 📑 Table of Contents

- [The Golden Rule — Build Order](#-the-golden-rule--build-order)
- [Step 1 — Design Your Entity](#step-1--design-your-entity)
- [Step 2 — Create Your DTOs](#step-2--create-your-dtos)
- [Step 3 — Create Your Repository](#step-3--create-your-repository)
- [Step 4 — Create Your Service Interface](#step-4--create-your-service-interface)
- [Step 5 — Create Your Implementation](#step-5--create-your-implementation)
- [Step 6 — Create Your Controller](#step-6--create-your-controller)
- [Step 7 — Custom Exceptions & Global Error Handling](#step-7--custom-exceptions--global-error-handling)
- [Step 8 — Utility Classes (Calculations & Helpers)](#step-8--utility-classes-calculations--helpers)
- [Implementation Logic Patterns](#-implementation-logic-patterns)
- [Entity Relationship Patterns](#-entity-relationship-patterns)
- [Repository Query Patterns](#-repository-query-patterns)
- [DTO Design Patterns](#-dto-design-patterns)
- [Validation Patterns](#-validation-patterns)
- [Complete Annotations Reference](#-complete-annotations-reference)

---

## 🎯 The Golden Rule — Build Order

Always build in this order. Each layer depends on the one below it:

```
 BUILD ORDER (bottom → top)         CALL ORDER (top → bottom)
 ─────────────────────────          ─────────────────────────
 1. Entity         (DB shape)       Client → Controller
 2. DTO            (API shape)               → Service Interface
 3. Repository     (DB access)               → Implementation
 4. Service        (Contract)                → Util (calculations)
 5. Implementation (Logic)                   → Repository
 6. Controller     (HTTP)                    → Database
 7. Exceptions     (Error handling)
 8. Util           (Calculations/helpers)
```

**Why this order?**
- Entity first → because everything maps to/from it
- DTO second → because the controller and implementation need it
- Repository third → because the implementation calls it
- Service interface fourth → because the controller calls it
- Implementation fifth → because it contains the actual logic
- Controller sixth → because it ties everything together
- Exceptions seventh → because implementations throw them and the GlobalExceptionHandler catches them
- Util eighth → because implementations call utility methods for calculations (can be created anytime)

---

## Step 1 — Design Your Entity

The entity maps directly to a **database table**. Every field becomes a **column**.

### Template

```java
package com.codeWithJeff.SampleSpringBootApplication.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity                                      // Marks this as a JPA entity (DB table)
@Table(name = "table_name")                  // Explicit table name (use plural, lowercase)
@Data                                        // Getters, setters, toString, equals, hashCode
@NoArgsConstructor                           // Required by JPA/Hibernate
@AllArgsConstructor                          // Required by @Builder
@Builder                                     // Builder pattern for clean object creation
public class YourEntity {

    @Id                                      // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
    private Long id;

    @Column(nullable = false, length = 100)  // NOT NULL, max 100 chars
    private String name;

    @Column(nullable = false, unique = true) // NOT NULL + UNIQUE constraint
    private String email;

    @Column(nullable = false)
    private Integer age;

    @Column(length = 500)                    // Nullable (default), max 500 chars
    private String description;
}
```

### Entity Design Checklist

```
□ @Entity + @Table(name = "plural_lowercase")
□ @Data + @NoArgsConstructor + @AllArgsConstructor + @Builder
□ @Id + @GeneratedValue(strategy = GenerationType.IDENTITY)
□ @Column constraints: nullable, unique, length
□ Table name is NOT a reserved keyword (user, order, group, table, key, index, check)
□ Table name is plural (students, teachers, users, orders)
□ Field types match DB types:
    - String → VARCHAR
    - Integer/int → INTEGER
    - Long/long → BIGINT
    - Double/double → DOUBLE PRECISION
    - Boolean/boolean → BOOLEAN
    - LocalDate → DATE
    - LocalDateTime → TIMESTAMP
    - BigDecimal → NUMERIC
```

### @Column Annotation Options

```java
@Column(
    name = "column_name",        // Override column name (default: field name in snake_case)
    nullable = false,            // NOT NULL constraint
    unique = true,               // UNIQUE constraint
    length = 100,                // VARCHAR max length (default: 255)
    columnDefinition = "TEXT",   // Raw SQL column type
    insertable = true,           // Include in INSERT (default: true)
    updatable = true             // Include in UPDATE (default: true)
)
```

### When to Use Each @GeneratedValue Strategy

| Strategy | SQL | Best for |
|---|---|---|
| `GenerationType.IDENTITY` | `BIGINT GENERATED BY DEFAULT AS IDENTITY` | PostgreSQL, H2, MySQL (auto-increment) |
| `GenerationType.SEQUENCE` | Uses DB sequence | PostgreSQL (better batch performance) |
| `GenerationType.AUTO` | Let Hibernate decide | Portable across databases |
| `GenerationType.UUID` | UUID primary key | Distributed systems, no sequential IDs |

---

## Step 2 — Create Your DTOs

DTOs (Data Transfer Objects) are the **shape of your API** — what the client sends and receives. They're separate from entities to:

1. **Hide internal fields** (e.g., don't expose `password` hash)
2. **Add validation** (e.g., `@NotBlank` on request, not on entity)
3. **Shape the response** (e.g., return `teacherName` instead of whole Teacher object)
4. **Decouple API from DB** (you can change the entity without breaking the API)

### Request DTO — What the Client Sends

```java
package com.codeWithJeff.SampleSpringBootApplication.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YourEntityRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 150, message = "Age must be at most 150")
    private Integer age;

    // NO id field — the server generates it
    // NO password field — handled separately
    // NO internal fields (createdAt, updatedAt, etc.)
}
```

### Response DTO — What the Client Receives

```java
package com.codeWithJeff.SampleSpringBootApplication.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YourEntityResponseDto {

    private Long id;              // Client needs the ID to reference this resource
    private String name;
    private String email;
    private Integer age;

    // Computed/resolved fields:
    private String teacherName;   // Instead of returning the whole Teacher object
    private String courseName;    // Flattened from a relationship

    // NO password field — never expose to client
    // NO internal fields unless the client needs them
}
```

### Why Separate Request and Response DTOs?

```
REQUEST DTO (what client sends):         RESPONSE DTO (what client gets):
┌──────────────────────────┐             ┌──────────────────────────┐
│ name: "John"     @NotBlank│            │ id: 1                    │  ← server generates
│ email: "j@x.com" @Email  │             │ name: "John"             │
│ age: 22          @Min(1) │              │ email: "j@x.com"        │
│                          │              │ age: 22                  │
│ (no id — server creates) │              │ teacherName: "Dr. Smith" │  ← resolved from FK
│ (no password — separate) │              │ courseName: "Math 101"   │  ← resolved from FK
└──────────────────────────┘              └──────────────────────────┘

Entity (DB row):
┌──────────────────────────────────────────────────┐
│ id: 1                                            │
│ name: "John"                                     │
│ email: "j@x.com"                                 │
│ age: 22                                          │
│ password: "$2a$10$hashedvalue..."   ← NEVER in DTO│
│ teacher_id: 5 (FK)                  ← resolved    │
│ created_at: 2026-04-08T10:00:00     ← internal    │
└──────────────────────────────────────────────────┘
```

### When to Use a Single DTO (vs Separate Request/Response)

| Scenario | Use |
|---|---|
| Fields are identical for input and output | Single DTO (like `SubjectDto`) |
| Response has extra fields (id, computed fields) | Separate Request + Response DTOs |
| Request needs validation but response doesn't | Separate DTOs |
| Simple CRUD with few fields | Single DTO is fine |
| Complex API with different shapes | Separate DTOs |

---

## Step 3 — Create Your Repository

The repository provides **database access** without writing SQL.

### Template

```java
package com.codeWithJeff.SampleSpringBootApplication.Repository;

import com.codeWithJeff.SampleSpringBootApplication.Entity.YourEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface YourEntityRepository extends JpaRepository<YourEntity, Long> {
    //                                                       ↑ Entity   ↑ ID type

    // Spring Data generates SQL from method names automatically:

    Optional<YourEntity> findByEmail(String email);
    // → SELECT * FROM your_entity WHERE email = ?

    boolean existsByEmail(String email);
    // → SELECT COUNT(*) > 0 FROM your_entity WHERE email = ?

    List<YourEntity> findByNameContainingIgnoreCase(String name);
    // → SELECT * FROM your_entity WHERE LOWER(name) LIKE LOWER('%?%')

    void deleteByEmail(String email);
    // → DELETE FROM your_entity WHERE email = ?
}
```

### Methods You Get for FREE from JpaRepository

| Method | SQL equivalent | Return type |
|---|---|---|
| `save(entity)` | `INSERT` or `UPDATE` | Entity |
| `findById(id)` | `SELECT * WHERE id = ?` | `Optional<Entity>` |
| `findAll()` | `SELECT *` | `List<Entity>` |
| `deleteById(id)` | `DELETE WHERE id = ?` | void |
| `existsById(id)` | `SELECT COUNT(*) > 0 WHERE id = ?` | boolean |
| `count()` | `SELECT COUNT(*)` | long |

### Spring Data Query Method Naming Convention

Spring generates SQL from method names. The formula is:

```
findBy + FieldName + Condition
existsBy + FieldName + Condition
countBy + FieldName + Condition
deleteBy + FieldName + Condition
```

**Examples:**

| Method name | Generated SQL |
|---|---|
| `findByEmail(String email)` | `WHERE email = ?` |
| `findByFirstName(String name)` | `WHERE first_name = ?` |
| `findByAgeGreaterThan(int age)` | `WHERE age > ?` |
| `findByAgeBetween(int min, int max)` | `WHERE age BETWEEN ? AND ?` |
| `findByNameContaining(String s)` | `WHERE name LIKE '%s%'` |
| `findByNameStartingWith(String s)` | `WHERE name LIKE 's%'` |
| `findByEmailIgnoreCase(String e)` | `WHERE LOWER(email) = LOWER(?)` |
| `findByRoleOrderByNameAsc(String r)` | `WHERE role = ? ORDER BY name ASC` |
| `findByActiveTrue()` | `WHERE active = true` |
| `existsByEmail(String email)` | `SELECT COUNT(*) > 0 WHERE email = ?` |
| `countByRole(String role)` | `SELECT COUNT(*) WHERE role = ?` |

**Combining conditions:**

| Keyword | Method | SQL |
|---|---|---|
| `And` | `findByNameAndEmail(...)` | `WHERE name = ? AND email = ?` |
| `Or` | `findByNameOrEmail(...)` | `WHERE name = ? OR email = ?` |
| `Not` | `findByNameNot(...)` | `WHERE name != ?` |
| `In` | `findByRoleIn(List<String>)` | `WHERE role IN (?, ?, ?)` |
| `IsNull` | `findByEmailIsNull()` | `WHERE email IS NULL` |
| `IsNotNull` | `findByEmailIsNotNull()` | `WHERE email IS NOT NULL` |

### Navigating Relationships in Query Methods

For entities with relationships (e.g., Subject has a Student FK):

```java
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // Navigate the relationship: Subject → Teacher → teacherId
    boolean existsByTeacher_TeacherId(Long teacherId);
    // → SELECT COUNT(*) > 0 FROM subject WHERE teacher_id = ?

    // Navigate: Subject → Student → studentId
    List<Subject> findByStudent_StudentId(Long studentId);
    // → SELECT * FROM subject WHERE student_id = ?

    boolean existsBySubject(String subjectName);
    // → SELECT COUNT(*) > 0 FROM subject WHERE subject = ?
}
```

**The underscore `_` navigates into a related entity's field:** `Teacher_TeacherId` means "go into the Teacher relationship, then access teacherId".

---

## Step 4 — Create Your Service Interface

The service interface is a **contract** — it defines *what* the service does, not *how*.

### Template

```java
package com.codeWithJeff.SampleSpringBootApplication.Service;

import com.codeWithJeff.SampleSpringBootApplication.dto.YourEntityRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.YourEntityResponseDto;
import java.util.List;

public interface YourEntityService {

    YourEntityResponseDto create(YourEntityRequestDto requestDto);
    List<YourEntityResponseDto> getAll();
    YourEntityResponseDto getById(Long id);
    YourEntityResponseDto update(Long id, YourEntityRequestDto requestDto);
    void delete(Long id);
}
```

### Why Use an Interface?

```
Controller → StudentService (interface) → StudentServiceImplementation (actual code)
                    ↑
         Loose coupling: Controller doesn't know or care about the implementation.
         You can swap implementations without changing the controller.
```

| Without Interface | With Interface |
|---|---|
| Controller depends on the concrete class | Controller depends on the abstraction |
| Changing implementation = changing controller | Changing implementation = transparent |
| Hard to test (need real implementation) | Easy to test (can mock the interface) |
| Tightly coupled | Loosely coupled |

---

## Step 5 — Create Your Implementation

The implementation contains **all the business logic**. This is where you validate, transform, and orchestrate.

### Template — Full CRUD Implementation

```java
package com.codeWithJeff.SampleSpringBootApplication.Implementation;

import com.codeWithJeff.SampleSpringBootApplication.Entity.YourEntity;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceAlreadyExistsException;
import com.codeWithJeff.SampleSpringBootApplication.Exceptions.ResourceNotFoundException;
import com.codeWithJeff.SampleSpringBootApplication.Repository.YourEntityRepository;
import com.codeWithJeff.SampleSpringBootApplication.Service.YourEntityService;
import com.codeWithJeff.SampleSpringBootApplication.dto.YourEntityRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.YourEntityResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service                          // Registers as a Spring bean
@RequiredArgsConstructor          // Constructor injection for all final fields
public class YourEntityServiceImpl implements YourEntityService {

    private final YourEntityRepository repository;
    // Add other repositories here if you need to look up related entities

    // ═══════════════════════════════════════════════════
    // CREATE — Validate → Build Entity → Save → Map to DTO
    // ═══════════════════════════════════════════════════
    @Override
    public YourEntityResponseDto create(YourEntityRequestDto requestDto) {

        // 1. VALIDATE — business rules before saving
        if (repository.existsByEmail(requestDto.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        // 2. BUILD ENTITY — map DTO fields to entity
        YourEntity entity = YourEntity.builder()
                .name(requestDto.getName())
                .email(requestDto.getEmail())
                .age(requestDto.getAge())
                .build();

        // 3. SAVE — persist to database
        YourEntity saved = repository.save(entity);

        // 4. MAP TO RESPONSE — return DTO (not entity)
        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════
    // READ ALL — Fetch → Map each → Return list
    // ═══════════════════════════════════════════════════
    @Override
    public List<YourEntityResponseDto> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)      // Convert each entity to DTO
                .toList();
    }

    // ═══════════════════════════════════════════════════
    // READ ONE — Find or throw 404 → Map to DTO
    // ═══════════════════════════════════════════════════
    @Override
    public YourEntityResponseDto getById(Long id) {
        YourEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + id));
        return toResponse(entity);
    }

    // ═══════════════════════════════════════════════════
    // UPDATE — Find or 404 → Validate → Update fields → Save → Map
    // ═══════════════════════════════════════════════════
    @Override
    public YourEntityResponseDto update(Long id, YourEntityRequestDto requestDto) {

        // 1. FIND existing entity (throw 404 if not found)
        YourEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + id));

        // 2. VALIDATE — check for conflicts with OTHER records
        //    (exclude the current record from the uniqueness check)
        if (!entity.getEmail().equals(requestDto.getEmail())
                && repository.existsByEmail(requestDto.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already taken");
        }

        // 3. UPDATE FIELDS — set new values
        entity.setName(requestDto.getName());
        entity.setEmail(requestDto.getEmail());
        entity.setAge(requestDto.getAge());

        // 4. SAVE — JPA detects changes and runs UPDATE
        YourEntity saved = repository.save(entity);

        // 5. MAP TO RESPONSE
        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════
    // DELETE — Find or 404 → Delete
    // ═══════════════════════════════════════════════════
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }
        repository.deleteById(id);
    }

    // ═══════════════════════════════════════════════════
    // HELPER — Entity → Response DTO mapping
    // ═══════════════════════════════════════════════════
    private YourEntityResponseDto toResponse(YourEntity entity) {
        return YourEntityResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .age(entity.getAge())
                .build();
    }
}
```

### The Implementation Pattern (Memorize This)

Every method follows the same structure:

```
┌──────────────────────────────────────────────────────────────┐
│              IMPLEMENTATION METHOD PATTERN                     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. VALIDATE   → Check business rules                        │
│                  - Does the resource exist? (for GET/UPDATE/DELETE) │
│                  - Is the data unique? (for CREATE/UPDATE)   │
│                  - Are relationships valid? (for linked entities)  │
│                                                              │
│  2. LOOKUP     → Fetch related entities from DB              │
│                  - findById() with .orElseThrow()            │
│                  - existsBy...() for uniqueness checks       │
│                                                              │
│  3. BUILD      → Create/update the entity                    │
│                  - Entity.builder()... for CREATE             │
│                  - entity.setField()... for UPDATE            │
│                                                              │
│  4. SAVE       → Persist to database                         │
│                  - repository.save(entity)                   │
│                                                              │
│  5. MAP        → Convert entity to response DTO              │
│                  - toResponse(entity) helper method           │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Common Logic Patterns

#### Pattern: Lookup with 404

```java
// The STANDARD pattern for finding an entity or throwing 404
YourEntity entity = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Not found with id: " + id));
```

#### Pattern: Uniqueness Check Before Save

```java
// Check BEFORE creating to give a clear 409 error instead of a DB constraint violation
if (repository.existsByEmail(request.getEmail())) {
    throw new ResourceAlreadyExistsException("Email already exists");
}
```

#### Pattern: Update Uniqueness Check (Exclude Self)

```java
// When updating, the CURRENT record's email is fine — only check OTHER records
if (!entity.getEmail().equals(requestDto.getEmail())
        && repository.existsByEmail(requestDto.getEmail())) {
    throw new ResourceAlreadyExistsException("Email already taken");
}
```

#### Pattern: Entity with Foreign Keys

```java
// When creating an entity that references other entities (like Subject → Student, Teacher)
public SubjectResponseDto createSubject(SubjectRequestDto request) {

    // 1. Validate uniqueness
    if (subjectRepository.existsBySubject(request.getSubject())) {
        throw new ResourceAlreadyExistsException("Subject already exists");
    }

    // 2. Look up related entities by their IDs
    Teacher teacher = teacherRepository.findById(request.getTeacherId())
        .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

    Student student = studentRepository.findById(request.getStudentId())
        .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

    // 3. Build entity with the FULL entity objects (not just IDs)
    Subject subject = Subject.builder()
        .teacher(teacher)          // Pass the full Teacher entity
        .student(student)          // Pass the full Student entity
        .subject(request.getSubject())
        .build();

    // 4. Save
    Subject saved = subjectRepository.save(subject);

    // 5. Map — resolve names from the related entities
    return SubjectResponseDto.builder()
        .subjectId(saved.getSubjectId())
        .teacherId(teacher.getTeacherId())
        .studentId(student.getStudentId())
        .subject(saved.getSubject())
        .teacherName(teacher.getFirstName() + " " + teacher.getLastName())
        .studentName(student.getFirstName() + " " + student.getLastName())
        .build();
}
```

---

## Step 6 — Create Your Controller

The controller is the **HTTP entry point**. It handles requests, delegates to the service, and returns responses.

### Template — Full CRUD Controller

```java
package com.codeWithJeff.SampleSpringBootApplication.Controller;

import com.codeWithJeff.SampleSpringBootApplication.Service.YourEntityService;
import com.codeWithJeff.SampleSpringBootApplication.dto.YourEntityRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.YourEntityResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController                              // REST endpoint (returns JSON)
@RequestMapping("/api/your-entities")        // Base URL
@RequiredArgsConstructor                     // Constructor injection
public class YourEntityController {

    private final YourEntityService service; // Inject the INTERFACE, not the implementation

    @PostMapping                             // POST /api/your-entities
    @ResponseStatus(HttpStatus.CREATED)      // Return 201 Created
    public YourEntityResponseDto create(@Valid @RequestBody YourEntityRequestDto request) {
        return service.create(request);
    }

    @GetMapping                              // GET /api/your-entities
    public List<YourEntityResponseDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")                     // GET /api/your-entities/{id}
    public YourEntityResponseDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")                     // PUT /api/your-entities/{id}
    public YourEntityResponseDto update(@PathVariable Long id,
                                        @Valid @RequestBody YourEntityRequestDto request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")                  // DELETE /api/your-entities/{id}
    @ResponseStatus(HttpStatus.NO_CONTENT)   // Return 204 No Content
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
```

### Controller Rules

| Rule | Why |
|---|---|
| **No business logic** in the controller | Logic belongs in the Implementation |
| **Inject the interface**, not the impl class | Loose coupling |
| **Use `@Valid`** on `@RequestBody` | Triggers DTO validation annotations |
| **Set appropriate `@ResponseStatus`** | 201 for create, 204 for delete |
| **Keep methods thin** — just delegate to service | Controller = HTTP handler, nothing more |

### HTTP Method → CRUD Mapping

| HTTP Method | CRUD | Annotation | Status Code | Has Request Body? |
|---|---|---|---|---|
| `POST` | Create | `@PostMapping` | `201 Created` | Yes |
| `GET` | Read | `@GetMapping` | `200 OK` | No |
| `PUT` | Update (full) | `@PutMapping` | `200 OK` | Yes |
| `PATCH` | Update (partial) | `@PatchMapping` | `200 OK` | Yes |
| `DELETE` | Delete | `@DeleteMapping` | `204 No Content` | No |

---

## Step 7 — Custom Exceptions & Global Error Handling

The Exceptions layer gives your API **clean, consistent error responses**. Instead of every error returning a generic `500 Internal Server Error`, you throw specific exceptions that get caught and translated into proper HTTP status codes.

### How the Exception System Works

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    EXCEPTION FLOW — HOW ERRORS BECOME HTTP RESPONSES       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  1. Implementation throws a custom exception:                              │
│     throw new ResourceNotFoundException("Student not found with id: 5")    │
│                                                                            │
│  2. Exception bubbles up through the call stack:                           │
│     Implementation → Service → Controller → Spring Framework               │
│                                                                            │
│  3. Spring sees @RestControllerAdvice (GlobalExceptionHandler):            │
│     "Is there a @ExceptionHandler that matches ResourceNotFoundException?" │
│     → YES: handleResourceNotFound() catches it                             │
│                                                                            │
│  4. Handler builds ErrorResponse and returns it with the correct status:   │
│     {                                                                      │
│       "timestamp": "2026-04-08T10:00:00",                                  │
│       "status": 404,                                                       │
│       "error": "Not Found",                                                │
│       "message": "Student not found with id: 5",                           │
│       "path": "/api/students/5"                                            │
│     }                                                                      │
│                                                                            │
│  WITHOUT GlobalExceptionHandler:                                           │
│     → RuntimeException → Spring returns generic 500 with no useful info    │
│                                                                            │
│  WITH GlobalExceptionHandler:                                              │
│     → Custom exception → Specific HTTP status + clear error message        │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### 7.1 — The Three Parts of Exception Handling

Your project's exception system has **three parts** that work together:

```
Exceptions/
├── ErrorResponse.java               ← The JSON shape of ALL error responses
├── ResourceNotFoundException.java    ← Custom exception class (one per error type)
├── ResourceAlreadyExistsException.java
├── StudentExceptions.java
├── UsernameNotFoundException.java
└── GlobalExceptionHandler.java       ← Catches exceptions, returns ErrorResponse
```

| Part | Purpose | Analogy |
|---|---|---|
| **Custom Exception** | A named error type — says *what* went wrong | The alarm label ("fire", "flood", "intruder") |
| **ErrorResponse** | The JSON structure the client receives | The alarm report form |
| **GlobalExceptionHandler** | Maps exception → HTTP status + ErrorResponse | The alarm dispatcher — routes each alarm type to the right response |

### 7.2 — ErrorResponse (The Standard Error Shape)

Every error your API returns should have the **same JSON structure**. This is your `ErrorResponse` DTO:

```java
package com.codeWithJeff.SampleSpringBootApplication.Exceptions;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Standard error response — ALL errors follow this structure.
 * The client always knows what to expect.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;    // When the error happened
    private int status;                 // HTTP status code (404, 409, 400, etc.)
    private String error;               // Short error label ("Not Found", "Conflict")
    private String message;             // Human-readable detail ("Student not found with id: 5")
    private String path;                // Which endpoint was called ("/api/students/5")
}
```

**Every error response looks like this:**
```json
{
  "timestamp": "2026-04-08T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Student not found with id: 5",
  "path": "/api/students/5"
}
```

### 7.3 — Custom Exception Classes

Each exception is a simple class that extends `RuntimeException`. It carries a message and nothing else — the `GlobalExceptionHandler` handles the HTTP mapping.

**Template:**
```java
package com.codeWithJeff.SampleSpringBootApplication.Exceptions;

public class YourCustomException extends RuntimeException {
    public YourCustomException(String message) {
        super(message);
    }
}
```

**Your project's current exceptions:**

| Exception class | HTTP Status | When to throw | Example |
|---|---|---|---|
| `ResourceNotFoundException` | `404 Not Found` | Entity not found in DB | `findById(id).orElseThrow(...)` |
| `ResourceAlreadyExistsException` | `409 Conflict` | Duplicate (uniqueness violation) | `existsByEmail()` returns true |
| `StudentExceptions` | `400 Bad Request` | Invalid student data | Custom business rule violation |
| `UsernameNotFoundException` | `404 Not Found` | User not found during auth | `findByEmail()` returns empty |
| `BadCredentialsException` | `401 Unauthorized` | Wrong password on login | Spring Security throws this |

**Why extend `RuntimeException` (not `Exception`)?**

| Base class | Type | Requires try/catch? | Used in Spring? |
|---|---|---|---|
| `RuntimeException` | Unchecked | No — propagates freely | ✅ Yes — Spring catches them in `@ExceptionHandler` |
| `Exception` | Checked | Yes — every caller must handle it | ❌ Not recommended — adds clutter everywhere |

**Rule:** Always extend `RuntimeException` for custom exceptions. Spring's `@ExceptionHandler` catches them cleanly without forcing `try/catch` blocks in every service method.

### 7.4 — GlobalExceptionHandler (The Dispatcher)

The `GlobalExceptionHandler` is the **central place** that maps every exception type to an HTTP response. It uses two key annotations:

| Annotation | Purpose |
|---|---|
| `@RestControllerAdvice` | Tells Spring: "This class handles exceptions for ALL controllers" |
| `@ExceptionHandler(XxxException.class)` | Tells Spring: "This method handles XxxException" |

**Full pattern — your actual GlobalExceptionHandler:**

```java
package com.codeWithJeff.SampleSpringBootApplication.Exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 Not Found ──────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())         // 404
                .error("Not Found")
                .message(ex.getMessage())                      // "Student not found with id: 5"
                .path(request.getRequestURI())                 // "/api/students/5"
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ── 409 Conflict ───────────────────────────────────────────
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(
            ResourceAlreadyExistsException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())           // 409
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ── 400 Bad Request ────────────────────────────────────────
    @ExceptionHandler(StudentExceptions.class)
    public ResponseEntity<ErrorResponse> handleStudentException(
            StudentExceptions ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())        // 400
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ── 404 User Not Found ─────────────────────────────────────
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlesUsernameNotFound(
            UsernameNotFoundException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())          // 404
                .error("User not found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ── 401 Unauthorized (wrong password) ──────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())       // 401
                .error("Unauthorized")
                .message("Invalid email or password")          // Don't expose which field was wrong
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}
```

### 7.5 — How to Add a New Exception (Step by Step)

**Example:** You want a `GradeOutOfRangeException` for grades not between 0-100.

**Step A — Create the exception class:**
```java
// Exceptions/GradeOutOfRangeException.java
package com.codeWithJeff.SampleSpringBootApplication.Exceptions;

public class GradeOutOfRangeException extends RuntimeException {
    public GradeOutOfRangeException(String message) {
        super(message);
    }
}
```

**Step B — Add a handler in GlobalExceptionHandler:**
```java
// Add this method inside GlobalExceptionHandler
@ExceptionHandler(GradeOutOfRangeException.class)
public ResponseEntity<ErrorResponse> handleGradeOutOfRange(
        GradeOutOfRangeException ex, HttpServletRequest request) {

    ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())       // 400
            .error("Bad Request")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
}
```

**Step C — Throw it in your Implementation:**
```java
// Inside GradesImplementation.createGrades()
if (gradesRequestDto.getGrade() < 0 || gradesRequestDto.getGrade() > 100) {
    throw new GradeOutOfRangeException("Grade must be between 0 and 100, got: " + gradesRequestDto.getGrade());
}
```

**Result — client gets:**
```json
{
  "timestamp": "2026-04-08T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Grade must be between 0 and 100, got: 150",
  "path": "/api/grades"
}
```

### 7.6 — Exception → HTTP Status Code Quick Reference

| HTTP Status | When to use | Exception name suggestion |
|---|---|---|
| `400 Bad Request` | Invalid input, business rule violation | `BadRequestException`, `GradeOutOfRangeException` |
| `401 Unauthorized` | Not logged in, wrong credentials | `BadCredentialsException` (Spring Security) |
| `403 Forbidden` | Logged in but wrong role | `AccessDeniedException` (Spring Security) |
| `404 Not Found` | Resource doesn't exist in DB | `ResourceNotFoundException` |
| `409 Conflict` | Duplicate / uniqueness violation | `ResourceAlreadyExistsException` |
| `422 Unprocessable Entity` | Valid format but semantic error | `UnprocessableEntityException` |
| `500 Internal Server Error` | Unexpected server error | Don't throw this intentionally — let it happen naturally |

### 7.7 — Exception Handling Best Practices

```
DO:
  ✅ Throw custom exceptions from Implementation layer
  ✅ Let GlobalExceptionHandler catch and format them
  ✅ Use specific exception types (ResourceNotFoundException, not RuntimeException)
  ✅ Include helpful messages ("Student not found with id: 5", not just "not found")
  ✅ Map to the correct HTTP status code
  ✅ Keep ErrorResponse structure consistent for ALL errors

DON'T:
  ❌ Throw exceptions from Controllers — delegate to Implementation
  ❌ Catch exceptions in Controllers — let GlobalExceptionHandler handle them
  ❌ Use generic RuntimeException — create a named exception
  ❌ Return error details about internals (stack traces, SQL queries, etc.)
  ❌ Use try/catch in Implementation unless you need to transform the exception
  ❌ Expose "Invalid password" vs "Invalid email" — just say "Invalid credentials"
```

---

## Step 8 — Utility Classes (Calculations & Helpers)

The `Util` package contains **stateless helper methods** for calculations, formatting, and transformations that don't belong in any specific service. Think of them as your toolbox — reusable functions that any service can call.

### When to Use Util vs Implementation

```
┌────────────────────────────────────────────────────────────────┐
│                   WHERE DOES THIS LOGIC BELONG?                │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  "Calculate the average grade for a student"                   │
│  → UTIL ✅  Pure math, no DB access, reusable                 │
│                                                                │
│  "Get the average grade for student #5"                        │
│  → IMPLEMENTATION ✅  Needs DB access (find student, get grades)│
│    Then calls UTIL for the calculation                         │
│                                                                │
│  "Format a student's full name"                                │
│  → UTIL ✅  String manipulation, reusable                     │
│                                                                │
│  "Create a new student in the database"                        │
│  → IMPLEMENTATION ✅  Business logic + DB write                │
│                                                                │
│  "Validate that an email is unique"                            │
│  → IMPLEMENTATION ✅  Needs DB access (repository.existsBy)    │
│                                                                │
│  "Check if a grade is within 0-100 range"                      │
│  → UTIL ✅  Pure validation, no DB needed                     │
│                                                                │
│  RULE: If it needs a Repository or other Spring bean → Implementation │
│        If it's a pure function (input → output) → Util        │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 8.1 — Your Project's Existing Util: GradeCalculator

**File:** `src/main/java/.../Util/GradeCalculator.java`

```java
package com.codeWithJeff.SampleSpringBootApplication.Util;

import com.codeWithJeff.SampleSpringBootApplication.Entity.Grades;
import lombok.experimental.UtilityClass;
import java.util.List;

@UtilityClass
public class GradeCalculator {

    public static double calculateAverageGrade(List<Grades> gradesList) {
        if (gradesList == null || gradesList.isEmpty()) {
            return 0.0;
        }
        return gradesList.stream()
                .mapToDouble(Grades::getGrade)
                .average()
                .orElse(0.0);
    }
}
```

**How it's used in GradesImplementation:**

```java
@Service
@RequiredArgsConstructor
public class GradesImplementation implements GradeService {

    private final GradesRepository gradesRepository;
    private final StudentRepository studentRepository;

    @Override
    public double getAverageGradeByStudentId(Long studentId) {
        // 1. VALIDATE — student exists (needs DB → Implementation's job)
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // 2. LOOKUP — get all grades for this student (needs DB → Implementation's job)
        List<Grades> gradesList = gradesRepository.findByStudent_StudentId(studentId);

        // 3. CALCULATE — pure math (no DB needed → Util's job)
        return GradeCalculator.calculateAverageGrade(gradesList);
    }
}
```

**Notice the separation:**
- **Implementation** handles DB access (find student, get grades) and orchestration
- **Util** handles the pure calculation (average of a list of numbers)
- If you need the same average calculation elsewhere, just call `GradeCalculator.calculateAverageGrade()` — no code duplication

### 8.2 — @UtilityClass Annotation (Lombok)

```java
@UtilityClass
public class GradeCalculator { ... }
```

`@UtilityClass` is a Lombok annotation that does three things:

| What it does | Why |
|---|---|
| Makes all methods `static` | Utility methods don't need an instance — call them directly: `GradeCalculator.calculate(...)` |
| Makes the class `final` | Prevents extending the utility class (no subclasses) |
| Adds a private constructor | Prevents instantiation — you can't do `new GradeCalculator()` |

**With `@UtilityClass`:**
```java
@UtilityClass
public class GradeCalculator {
    public double calculateAverage(List<Grades> grades) { ... }  // automatically static
}

// Usage — call directly on the class (no instance needed):
double avg = GradeCalculator.calculateAverage(gradesList);
```

**Without `@UtilityClass` (manual equivalent):**
```java
public final class GradeCalculator {
    private GradeCalculator() {}  // prevent instantiation
    public static double calculateAverage(List<Grades> grades) { ... }
}
```

### 8.3 — Util Template

Use this template when creating a new utility class:

```java
package com.codeWithJeff.SampleSpringBootApplication.Util;

import lombok.experimental.UtilityClass;

/**
 * [Describe what this utility does]
 *
 * Called by: [which Implementation(s) use it]
 * Example:  ClassName.methodName(input)
 */
@UtilityClass
public class YourUtilityName {

    /**
     * [Describe what this method calculates/does]
     *
     * @param input [describe the input]
     * @return [describe the output]
     */
    public double yourCalculation(SomeType input) {
        // 1. Handle edge cases (null, empty)
        if (input == null) {
            return 0.0;
        }

        // 2. Perform the calculation
        return /* calculation */;
    }
}
```

### 8.4 — Common Util Examples

Here are utility classes you might need in a real project:

#### GradeCalculator — Averages, GPA, Pass/Fail

```java
@UtilityClass
public class GradeCalculator {

    /** Average of all grades in the list */
    public double calculateAverageGrade(List<Grades> gradesList) {
        if (gradesList == null || gradesList.isEmpty()) return 0.0;
        return gradesList.stream()
                .mapToDouble(Grades::getGrade)
                .average()
                .orElse(0.0);
    }

    /** Highest grade in the list */
    public double calculateHighestGrade(List<Grades> gradesList) {
        if (gradesList == null || gradesList.isEmpty()) return 0.0;
        return gradesList.stream()
                .mapToDouble(Grades::getGrade)
                .max()
                .orElse(0.0);
    }

    /** Lowest grade in the list */
    public double calculateLowestGrade(List<Grades> gradesList) {
        if (gradesList == null || gradesList.isEmpty()) return 0.0;
        return gradesList.stream()
                .mapToDouble(Grades::getGrade)
                .min()
                .orElse(0.0);
    }

    /** Count how many grades are passing (≥ 60) */
    public long countPassingGrades(List<Grades> gradesList) {
        if (gradesList == null) return 0;
        return gradesList.stream()
                .filter(g -> g.getGrade() >= 60.0)
                .count();
    }

    /** Convert numeric grade to letter grade */
    public String toLetterGrade(double grade) {
        if (grade >= 90) return "A";
        if (grade >= 80) return "B";
        if (grade >= 70) return "C";
        if (grade >= 60) return "D";
        return "F";
    }
}
```

#### NameFormatter — String Formatting

```java
@UtilityClass
public class NameFormatter {

    /** Combine first + last name */
    public String fullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    /** "john doe" → "John Doe" */
    public String capitalize(String name) {
        if (name == null || name.isBlank()) return name;
        return Arrays.stream(name.trim().split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /** Generate initials: "John Doe" → "JD" */
    public String initials(String firstName, String lastName) {
        return (firstName.substring(0, 1) + lastName.substring(0, 1)).toUpperCase();
    }
}
```

#### DateUtils — Date Calculations

```java
@UtilityClass
public class DateUtils {

    /** Calculate age from birthdate */
    public int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /** Check if a date is in the current semester */
    public boolean isCurrentSemester(LocalDate date) {
        int month = LocalDate.now().getMonthValue();
        int semesterStart = (month <= 6) ? 1 : 7;
        int semesterEnd = (month <= 6) ? 6 : 12;
        return date.getMonthValue() >= semesterStart && date.getMonthValue() <= semesterEnd;
    }
}
```

### 8.5 — Util vs @Service vs @Component — When to Use What

| Pattern | Annotation | Instance? | Inject dependencies? | Use when |
|---|---|---|---|---|
| **Util class** | `@UtilityClass` | No (all static) | ❌ No | Pure functions: math, formatting, parsing |
| **Service** | `@Service` | Yes (Spring bean) | ✅ Yes (repositories, other services) | Business logic that needs DB access |
| **Component** | `@Component` | Yes (Spring bean) | ✅ Yes | General-purpose beans (filters, listeners) |

**The key question:** Does the method need to access the database or call another Spring bean?
- **Yes** → Put it in an `@Service` (Implementation)
- **No** → Put it in a `@UtilityClass` (Util)

### 8.6 — How Util Fits in the Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     REQUEST FLOW WITH UTIL                    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Controller                                                  │
│  │  @GetMapping("/api/grades/average/{studentId}")           │
│  │  return service.getAverageGradeByStudentId(studentId)     │
│  │                                                           │
│  ▼                                                           │
│  Implementation (GradesImplementation)                        │
│  │  1. Validate student exists  → studentRepository          │
│  │  2. Fetch grades            → gradesRepository            │
│  │  3. Calculate average       → GradeCalculator (UTIL) ◄──── Pure calculation
│  │  4. Return result                                         │
│  │                                                           │
│  ▼                                                           │
│  Util (GradeCalculator)                                      │
│     calculateAverageGrade(gradesList)                        │
│     → stream → mapToDouble → average → return                │
│     ↑                                                        │
│     No @Service, no @Autowired, no DB access                 │
│     Just input → output                                      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 8.7 — Util Best Practices

```
DO:
  ✅ Use @UtilityClass (Lombok) — prevents instantiation, makes methods static
  ✅ Handle null/empty inputs — return sensible defaults (0.0, empty string, etc.)
  ✅ Keep methods pure — same input always produces same output
  ✅ Name clearly — GradeCalculator.calculateAverageGrade(), not Util.calc()
  ✅ Group related methods — all grade calculations in GradeCalculator, all name formatting in NameFormatter
  ✅ Add Javadoc — describe input, output, and edge cases
  ✅ Write unit tests — Util methods are the EASIEST to unit test (no mocking needed)

DON'T:
  ❌ Inject repositories or services into a Util class — use @Service instead
  ❌ Put DB access in Util — that's the Implementation's job
  ❌ Create a single giant "Utils" class — split by domain (GradeCalculator, NameFormatter, DateUtils)
  ❌ Put business rules in Util — Util calculates, Implementation decides
  ❌ Use @Component or @Service on a Util class — it's a static helper, not a Spring bean
```

---

## 🔁 Entity Relationship Patterns

### @ManyToOne — Many Children, One Parent

**Example:** Many Subjects can reference one Student.

```java
// Subject.java (the "many" side — has the FK column)
@ManyToOne
@JoinColumn(name = "student_id", nullable = false)
private Student student;

// Generated SQL: ALTER TABLE subject ADD COLUMN student_id BIGINT REFERENCES students(student_id)
```

**DB result:**
```
subjects table:
| subject_id | subject    | student_id (FK) |
|------------|-----------|-----------------|
| 1          | Math      | 1               |
| 2          | Science   | 1               |  ← same student
| 3          | English   | 2               |
```

### @OneToOne — One to One, Unique FK

**Example:** Each Teacher teaches exactly one Subject.

```java
// Subject.java (the owning side — has the FK column)
@OneToOne
@JoinColumn(name = "teacher_id", nullable = false, unique = true)
private Teacher teacher;

// Generated SQL: ALTER TABLE subject ADD COLUMN teacher_id BIGINT UNIQUE REFERENCES teachers(teacher_id)
```

### @ManyToMany — Many to Many, Join Table

**Example:** Students can enroll in many Courses, and Courses have many Students.

```java
// Student.java
@ManyToMany
@JoinTable(
    name = "student_courses",                              // Join table name
    joinColumns = @JoinColumn(name = "student_id"),        // FK to this entity
    inverseJoinColumns = @JoinColumn(name = "course_id")   // FK to the other entity
)
private Set<Course> courses = new HashSet<>();

// Course.java (inverse side)
@ManyToMany(mappedBy = "courses")     // "courses" is the field name in Student
private Set<Student> students = new HashSet<>();
```

### @OneToMany — One Parent, Many Children (Bidirectional)

```java
// Student.java (the "one" side)
@OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
private List<Subject> subjects = new ArrayList<>();

// Subject.java (the "many" side — has the FK)
@ManyToOne
@JoinColumn(name = "student_id")
private Student student;
```

### Relationship Cheat Sheet

| Annotation | FK Location | Example |
|---|---|---|
| `@ManyToOne` | **This** table has the FK column | Subject table has `student_id` FK |
| `@OneToOne` | **This** table has the FK column (unique) | Subject table has `teacher_id` FK (unique) |
| `@OneToMany` | **Other** table has the FK column | Student doesn't have FK; Subject does |
| `@ManyToMany` | **Join table** has both FK columns | `student_courses` table has both FKs |

### Fetch Types

```java
@ManyToOne(fetch = FetchType.LAZY)     // Load relationship ONLY when accessed
@ManyToOne(fetch = FetchType.EAGER)    // Load relationship immediately with parent
```

| Fetch type | Default for | Behavior |
|---|---|---|
| `EAGER` | `@ManyToOne`, `@OneToOne` | Always loads — can cause performance issues with deep chains |
| `LAZY` | `@OneToMany`, `@ManyToMany` | Loads on-demand — better performance, but needs open session |

**Rule of thumb:** Use `LAZY` for collections (`@OneToMany`, `@ManyToMany`). Use `EAGER` (default) for single-entity relationships (`@ManyToOne`, `@OneToOne`) unless performance is a concern.

---

## 🔍 Repository Query Patterns

### Beyond Method Names — @Query for Complex Queries

When the method name gets too long, use `@Query`:

```java
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // JPQL query (uses entity/field names, not table/column names)
    @Query("SELECT s FROM Student s WHERE s.age > :minAge AND s.course = :course")
    List<Student> findSeniorStudentsByCourse(@Param("minAge") int minAge,
                                              @Param("course") String course);

    // Native SQL query (uses actual table/column names)
    @Query(value = "SELECT * FROM students WHERE age > ?1 AND course = ?2",
           nativeQuery = true)
    List<Student> findSeniorStudentsByCourseSql(int minAge, String course);

    // Count query
    @Query("SELECT COUNT(s) FROM Student s WHERE s.course = :course")
    long countByCourse(@Param("course") String course);

    // Update query (requires @Modifying + @Transactional)
    @Modifying
    @Transactional
    @Query("UPDATE Student s SET s.course = :newCourse WHERE s.course = :oldCourse")
    int updateCourse(@Param("oldCourse") String oldCourse,
                     @Param("newCourse") String newCourse);
}
```

### Pagination and Sorting

```java
// Repository method with Pageable
Page<Student> findByCourse(String course, Pageable pageable);

// Controller usage
@GetMapping
public Page<StudentResponseDto> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "studentId") String sortBy) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
    return service.getAll(pageable);
}
```

---

## ✅ Validation Patterns

### Request DTO Validation

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequestDto {

    @NotBlank(message = "First name is required")         // String not null/empty/blank
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")              // Must be valid email
    private String email;

    @NotNull(message = "Age is required")                 // Integer/Long not null
    @Min(value = 1, message = "Age must be at least 1")   // Minimum value
    @Max(value = 150, message = "Age must be at most 150") // Maximum value
    private Integer age;

    @NotBlank(message = "Course is required")
    @Size(min = 2, max = 100, message = "Course must be 2-100 characters")
    private String course;
}
```

### Validation Annotation Quick Reference

| Annotation | Applies to | Rule |
|---|---|---|
| `@NotNull` | Any type | Cannot be null |
| `@NotBlank` | String | Cannot be null, empty `""`, or whitespace `"  "` |
| `@NotEmpty` | String, Collection | Cannot be null or empty (but whitespace is ok for String) |
| `@Email` | String | Must be a valid email format |
| `@Size(min, max)` | String, Collection | Length/size must be within range |
| `@Min(value)` | Number | Must be ≥ value |
| `@Max(value)` | Number | Must be ≤ value |
| `@Positive` | Number | Must be > 0 |
| `@PositiveOrZero` | Number | Must be ≥ 0 |
| `@Past` | Date/DateTime | Must be in the past |
| `@Future` | Date/DateTime | Must be in the future |
| `@Pattern(regexp)` | String | Must match regex pattern |

### Triggering Validation

Validation only runs when you use `@Valid` on the controller parameter:

```java
// ✅ Validation runs — @Valid triggers @NotBlank, @Email, etc.
public ResponseDto create(@Valid @RequestBody RequestDto request)

// ❌ Validation does NOT run — missing @Valid
public ResponseDto create(@RequestBody RequestDto request)
```

---

## 📚 Complete Annotations Reference

### Spring Web (Controllers)

| Annotation | Target | Purpose | Example |
|---|---|---|---|
| `@RestController` | Class | Marks a REST controller (returns JSON) | Class-level |
| `@Controller` | Class | Marks a view controller (returns HTML) | Class-level |
| `@RequestMapping("/path")` | Class/Method | Base URL path | `@RequestMapping("/api/students")` |
| `@GetMapping` | Method | Handles GET requests | `@GetMapping("/{id}")` |
| `@PostMapping` | Method | Handles POST requests | `@PostMapping` |
| `@PutMapping` | Method | Handles PUT requests | `@PutMapping("/{id}")` |
| `@DeleteMapping` | Method | Handles DELETE requests | `@DeleteMapping("/{id}")` |
| `@PatchMapping` | Method | Handles PATCH requests | `@PatchMapping("/{id}")` |
| `@PathVariable` | Parameter | Extracts URL path variable | `@PathVariable Long id` |
| `@RequestParam` | Parameter | Extracts query parameter | `@RequestParam String name` |
| `@RequestBody` | Parameter | Parses JSON body to object | `@RequestBody StudentDto dto` |
| `@ResponseStatus` | Method | Sets HTTP response status | `@ResponseStatus(HttpStatus.CREATED)` |
| `@Valid` | Parameter | Triggers bean validation | `@Valid @RequestBody StudentDto dto` |
| `@CrossOrigin` | Class/Method | Enable CORS for specific endpoints | `@CrossOrigin(origins = "http://localhost:3000")` |

### Spring Bean/Component

| Annotation | Target | Purpose | When to Use |
|---|---|---|---|
| `@Component` | Class | Generic Spring-managed bean | General-purpose components |
| `@Service` | Class | Service layer bean | Business logic classes |
| `@Repository` | Class | Data access bean | JPA repository interfaces |
| `@Configuration` | Class | Configuration class | Classes with @Bean methods |
| `@Bean` | Method | Declares a bean created by this method | Inside @Configuration classes |
| `@Autowired` | Field/Constructor | Inject a dependency | ⚠️ Prefer constructor injection |
| `@RequiredArgsConstructor` | Class | Generate constructor for `final` fields | ✅ Best practice for DI |
| `@Value("${key}")` | Field | Inject a property value | Reading from application.yml |
| `@Qualifier("name")` | Field/Parameter | Choose which bean to inject (when multiple exist) | Disambiguating beans |
| `@Primary` | Class | Mark as the preferred bean of its type | Default bean when multiple exist |
| `@Lazy` | Class/Field | Create bean only when first needed | Breaking circular dependencies |
| `@Scope("prototype")` | Class | New instance every time (default is singleton) | Stateful beans |

### Spring Boot

| Annotation | Target | Purpose |
|---|---|---|
| `@SpringBootApplication` | Main class | Combines @Configuration + @ComponentScan + @EnableAutoConfiguration |
| `@EnableAutoConfiguration` | Class | Auto-configure based on classpath dependencies |
| `@ComponentScan` | Class | Scan for @Component, @Service, @Repository, etc. |
| `@ConditionalOnProperty` | Class/Method | Only create bean if a property exists/matches |
| `@Profile("dev")` | Class/Method | Only active in the specified profile |

### JPA/Persistence

| Annotation | Target | Purpose | Example |
|---|---|---|---|
| `@Entity` | Class | Marks as a JPA entity (DB table) | Class-level |
| `@Table(name = "x")` | Class | Explicit table name | `@Table(name = "students")` |
| `@Id` | Field | Primary key | `private Long id` |
| `@GeneratedValue` | Field | Auto-generate PK value | `strategy = GenerationType.IDENTITY` |
| `@Column` | Field | Column constraints | `@Column(nullable = false, unique = true)` |
| `@ManyToOne` | Field | Many-to-one FK relationship | `private Student student` |
| `@OneToOne` | Field | One-to-one FK relationship | `private Teacher teacher` |
| `@OneToMany` | Field | One-to-many (inverse side) | `private List<Subject> subjects` |
| `@ManyToMany` | Field | Many-to-many relationship | `private Set<Course> courses` |
| `@JoinColumn` | Field | FK column configuration | `@JoinColumn(name = "student_id")` |
| `@JoinTable` | Field | Join table for @ManyToMany | See ManyToMany section |
| `@Transient` | Field | Exclude field from DB | Computed fields |
| `@Enumerated` | Field | Map Java enum to DB | `@Enumerated(EnumType.STRING)` |
| `@CreationTimestamp` | Field | Auto-set on create | `private LocalDateTime createdAt` |
| `@UpdateTimestamp` | Field | Auto-set on update | `private LocalDateTime updatedAt` |
| `@Modifying` | Method | Mark query as INSERT/UPDATE/DELETE | On @Query repository methods |
| `@Transactional` | Class/Method | Wrap in a DB transaction | Service methods that modify data |

### Validation (Jakarta Bean Validation)

| Annotation | Applies to | Rule |
|---|---|---|
| `@NotNull` | Any | Cannot be null |
| `@NotBlank` | String | Cannot be null/empty/whitespace |
| `@NotEmpty` | String, Collection | Cannot be null or empty |
| `@Email` | String | Valid email format |
| `@Size(min, max)` | String, Collection | Length within range |
| `@Min(value)` | Number | Minimum value |
| `@Max(value)` | Number | Maximum value |
| `@Positive` | Number | Must be > 0 |
| `@Pattern(regexp)` | String | Must match regex |
| `@Past` / `@Future` | Date | Must be in past/future |
| `@Valid` | Parameter | Triggers nested validation |

### Lombok

| Annotation | Generates |
|---|---|
| `@Data` | Getters + Setters + toString + equals + hashCode + @RequiredArgsConstructor |
| `@Getter` / `@Setter` | Just getters or setters |
| `@NoArgsConstructor` | No-argument constructor |
| `@AllArgsConstructor` | All-argument constructor |
| `@RequiredArgsConstructor` | Constructor for `final` fields |
| `@Builder` | Builder pattern (`.builder().field(x).build()`) |
| `@Slf4j` | Adds `log` field for logging (`log.info(...)`) |
| `@ToString` | toString() method |
| `@EqualsAndHashCode` | equals() and hashCode() methods |
| `@UtilityClass` | Makes class `final`, all methods `static`, adds private constructor — use for Util classes |

### Exception Handling

| Annotation | Target | Purpose | Example |
|---|---|---|---|
| `@RestControllerAdvice` | Class | Global exception handler for all controllers | `GlobalExceptionHandler` class |
| `@ControllerAdvice` | Class | Same but for non-REST controllers (returns views) | Rarely used in REST APIs |
| `@ExceptionHandler(X.class)` | Method | Catches exception type X and handles it | `@ExceptionHandler(ResourceNotFoundException.class)` |
| `@ResponseStatus(HttpStatus.X)` | Exception class | Auto-map exception to HTTP status (no handler needed) | `@ResponseStatus(HttpStatus.NOT_FOUND)` |

**`@RestControllerAdvice` vs `@ExceptionHandler` flow:**
```
Exception thrown → Spring searches for matching @ExceptionHandler
→ Found in @RestControllerAdvice class → executes handler method → returns ResponseEntity
→ NOT found → Spring returns default 500 Internal Server Error
```

**Alternative: `@ResponseStatus` on the exception itself (simpler but less flexible):**
```java
// Option A: @ResponseStatus on the exception (simple — no handler needed)
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
// Spring auto-returns 404 when this exception is thrown — but no custom JSON body

// Option B: @ExceptionHandler in GlobalExceptionHandler (recommended — full control)
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handle(...) { ... }
// Returns your custom ErrorResponse JSON with timestamp, message, path, etc.
```

### Spring Security

| Annotation | Target | Purpose |
|---|---|---|
| `@EnableWebSecurity` | Config class | Enable Spring Security web configuration |
| `@EnableMethodSecurity` | Config class | Enable @PreAuthorize on methods |
| `@PreAuthorize("expr")` | Method | Check authorization before method executes |
| `@PostAuthorize("expr")` | Method | Check authorization after method executes |
| `@Secured("ROLE_ADMIN")` | Method | Simpler role check (no SpEL) |

### Spring Cloud / Eureka

| Annotation | Target | Purpose |
|---|---|---|
| `@EnableEurekaServer` | Main class | Makes app a Eureka registry server |
| `@EnableDiscoveryClient` | Main class | Registers app with Eureka (auto-configured in Boot 3) |
| `@EnableFeignClients` | Main class | Enables declarative Feign HTTP clients |
| `@FeignClient(name = "x")` | Interface | Declares a Feign client for service "x" |
| `@LoadBalanced` | Bean method | Enables service name resolution via Eureka |

### Swagger/OpenAPI

| Annotation | Target | Purpose |
|---|---|---|
| `@Tag(name = "x")` | Class | Group endpoints in Swagger UI |
| `@Operation(summary = "x")` | Method | Describe an endpoint |
| `@Parameter(description = "x")` | Parameter | Describe a parameter |
| `@Schema(description = "x")` | Field | Describe a DTO field |
| `@SecurityRequirement(name = "x")` | Class/Method | Require auth in Swagger UI |

---

## 🎯 New Feature Checklist — Copy This Every Time

When building a new feature, copy this checklist and check off each step:

```
NEW FEATURE: _______________

□ 1. ENTITY
   □ @Entity + @Table(name = "plural_lowercase")
   □ @Data + @NoArgsConstructor + @AllArgsConstructor + @Builder
   □ @Id + @GeneratedValue(strategy = GenerationType.IDENTITY)
   □ @Column constraints (nullable, unique, length)
   □ Relationships (@ManyToOne, @OneToOne, etc.)
   □ Table name is NOT a reserved keyword

□ 2. DTOs
   □ RequestDto — with validation annotations (@NotBlank, @Email, etc.)
   □ ResponseDto — with ID + computed/resolved fields
   □ @Data + @NoArgsConstructor + @AllArgsConstructor + @Builder

□ 3. REPOSITORY
   □ extends JpaRepository<Entity, Long>
   □ @Repository
   □ Custom query methods (findByX, existsByX)

□ 4. SERVICE INTERFACE
   □ Method signatures for CRUD operations
   □ Uses DTOs (not entities) in method signatures

□ 5. IMPLEMENTATION
   □ @Service + @RequiredArgsConstructor
   □ implements ServiceInterface
   □ Inject Repository (and other repos for relationships)
   □ Each method: VALIDATE → LOOKUP → BUILD → SAVE → MAP
   □ Throw custom exceptions (not generic RuntimeException)
   □ Call Util classes for calculations (if needed)
   □ toResponse() helper method

□ 6. CONTROLLER
   □ @RestController + @RequestMapping
   □ @RequiredArgsConstructor → inject SERVICE INTERFACE
   □ @Valid on @RequestBody
   □ @ResponseStatus (201 for POST, 204 for DELETE)
   □ Methods are thin — just delegate to service

□ 7. EXCEPTIONS (if your feature has new error cases)
   □ Custom exception class → extends RuntimeException
   □ @ExceptionHandler in GlobalExceptionHandler → correct HTTP status
   □ ErrorResponse follows the standard shape (timestamp, status, error, message, path)
   □ Throw from Implementation, never from Controller

□ 8. UTIL (if your feature needs calculations/formatting)
   □ @UtilityClass → all methods static, no instantiation
   □ Pure functions — no DB access, no Spring beans
   □ Handle null/empty inputs with sensible defaults
   □ Called from Implementation, not Controller

□ 9. SECURITY (if needed)
   □ Add endpoint to SecurityConfig (permitAll / authenticated / hasRole)
   □ Add /error to permitAll (if not already)

□ 10. TEST
   □ Hit the endpoint via Swagger UI or Postman
   □ Test happy path + error cases (404, 409, 400)
   □ Test Util methods directly (pure functions are easy to test)
```

---

**Document Version:** 1.1
**Created:** April 8, 2026
**Updated:** April 8, 2026 — Added comprehensive Exceptions (Step 7) and Utility Classes (Step 8) sections with full patterns, templates, best practices, and real project examples.
**Context:** Comprehensive development patterns guide for building features in this Spring Boot project's layered architecture.

