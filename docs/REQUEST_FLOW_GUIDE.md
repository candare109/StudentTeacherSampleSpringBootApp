# Request Flow Guide (Student GET Example)

Use this guide to understand how one request moves through your layered structure, then copy the same pattern for a new controller.

## 1) Example Endpoint

- HTTP method: `GET`
- Path: `/api/students/{id}`
- Controller method: `StudentController#getStudentById(Long id)`

## 2) End-to-End Flow (`GET /api/students/{id}`)

1. Client calls `GET /api/students/1`.
2. `StudentController` receives the request and extracts `id` from `@PathVariable`.
3. Controller calls `StudentService#getStudentById(id)` (interface only, loose coupling).
4. `StudentServiceImplementation` executes the business logic.
5. Implementation calls `StudentRepository#findById(id)`.
6. `StudentRepository` (JPA) queries the `students` table using `Student` entity mapping.
7. If record exists: map `Student` entity to `StudentResponseDto`.
8. If record does not exist: throw `ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found")`.
9. Controller returns JSON response to the client.

## 3) Sequence View

```text
Client
  -> StudentController#getStudentById(id)
  -> StudentService#getStudentById(id)
  -> StudentServiceImplementation#getStudentById(id)
  -> StudentRepository#findById(id)
  -> DB table: students
  <- Student entity (or empty)
  <- StudentResponseDto (or 404)
  <- HTTP response
```

## 4) Data Mapping in This Flow

- Read model from DB: `Entity.Student`
- API response model: `dto.StudentResponseDto`
- Mapping location: `StudentServiceImplementation#toResponse(Student student)`

## 5) Request/Response Samples

### Request

```http
GET /api/students/1 HTTP/1.1
Host: localhost:7000
Accept: application/json
```

### Success Response (200)

```json
{
  "id": 1,
  "firstName": "Andrew",
  "lastName": "Candare",
  "email": "andrew@example.com",
  "age": 22,
  "course": "Computer Science"
}
```

### Not Found Response (404)

```json
{
  "timestamp": "2026-03-30T10:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Student not found",
  "path": "/api/students/999"
}
```

## 6) Reusable Pattern To Add Another Controller

Follow this checklist when creating a new feature (example: `Teacher`):

1. Create entity in `Entity` package (`Teacher`).
2. Create DTOs in `dto` package (`TeacherRequestDto`, `TeacherResponseDto`).
3. Create repository in `Repository` package (`TeacherRepository extends JpaRepository<Teacher, Long>`).
4. Create service interface in `Service` package (`TeacherService`).
5. Create implementation in `Implementation` package (`TeacherServiceImplementation`).
6. Add mapping methods (`toResponse`, optionally `toEntity`).
7. Create REST controller in `Controller` package (`TeacherController`).
8. Add CRUD endpoints and call only service interface from controller.
9. Add at least one integration test for create/read flow.

## 7) Minimal Skeleton (Copy Pattern)

```java
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/{id}")
    public TeacherResponseDto getById(@PathVariable Long id) {
        return teacherService.getById(id);
    }
}
```

Keep the same principle: Controller handles HTTP, Service defines contract, Implementation contains logic, Repository handles DB, DTOs are API payloads, Entity is table schema.

