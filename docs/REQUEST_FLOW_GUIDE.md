# Request Flow Guide

Use this guide to understand how one request moves through your layered structure, then copy the same pattern for a new controller.

**Last Updated:** April 1, 2026

## 1) Example Endpoint — Student GET

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

---

## 6) Example Endpoint — Subject POST (NEW — April 1, 2026)

- HTTP method: `POST`
- Path: `/api/subject`
- Controller method: `SubjectController#createSubject(SubjectDto requestSubjectDto)`

### End-to-End Flow (`POST /api/subject`)

1. Client calls `POST /api/subject` with JSON body: `{ "studentId": 1, "teacherId": 2, "subject": "Math" }`.
2. `SubjectController` receives the request, `@Valid` triggers DTO validation (`@NotNull`, `@NotBlank`).
3. Controller calls `SubjectService#createSubject(requestDto)` (interface only, loose coupling).
4. `SubjectServiceImplementation` executes the business logic:
   - **Validation 1:** `subjectRepository.existsBySubject("Math")` → if `true`: throw 409 CONFLICT.
   - **Validation 2:** `subjectRepository.existsByTeacher_TeacherId(2)` → if `true`: throw 409 CONFLICT.
   - **Lookup 1:** `teacherRepository.findById(2)` → if empty: throw 404 NOT_FOUND.
   - **Lookup 2:** `studentRepository.findById(1)` → if empty: throw 404 NOT_FOUND.
5. Build `Subject` entity using `Student` and `Teacher` entities fetched from DB.
6. `subjectRepository.save(subject)` → JPA inserts row with FK references.
7. Build response `SubjectDto` with resolved names (`studentName`, `teacherName`).
8. Controller returns JSON response to the client.

### Sequence View

```text
Client
  -> SubjectController#createSubject(SubjectDto)
  -> SubjectService#createSubject(SubjectDto)
  -> SubjectServiceImplementation#createSubject(SubjectDto)
     -> SubjectRepository#existsBySubject("Math")           ← validation
     -> SubjectRepository#existsByTeacher_TeacherId(2)      ← validation
     -> TeacherRepository#findById(2)                       ← lookup
     -> StudentRepository#findById(1)                       ← lookup
     -> SubjectRepository#save(subject)                     ← persist
  -> DB tables: subject (with FK to students + teachers)
  <- Subject entity (with auto-generated subjectId)
  <- SubjectDto (with resolved studentName + teacherName)
  <- HTTP response (201 Created)
```

### Request

```http
POST /api/subject HTTP/1.1
Host: localhost:7000
Content-Type: application/json

{
  "studentId": 1,
  "teacherId": 2,
  "subject": "Mathematics"
}
```

### Success Response (201 Created)

```json
{
  "subjectId": 1,
  "studentId": 1,
  "teacherId": 2,
  "subject": "Mathematics",
  "studentName": "Andrew Candare",
  "teacherName": "John Smith"
}
```

### Conflict Response (409)

```json
{
  "timestamp": "2026-04-01T10:00:00.000+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Subject already exists",
  "path": "/api/subject"
}
```

### Key Difference — Subject vs Student/Teacher CREATE

| Aspect | Student/Teacher | Subject |
|---|---|---|
| Entity build | Direct from DTO fields | Requires DB lookups first (fetch Student & Teacher entities) |
| Validations | Simple (email uniqueness) | Multiple (subject exists, teacher assigned, teacher exists, student exists) |
| FK references | None | `student_id` + `teacher_id` foreign keys |
| Response mapping | Direct field copy | Resolves names from related entities |

---

## 7) Reusable Pattern To Add Another Controller

Follow this checklist when creating a new feature (example: `Subject`):

1. Create entity in `Entity` package (`Subject`) — define fields, relationships (`@ManyToOne`, `@OneToOne`).
2. Create DTOs in `dto` package (`SubjectDto` or separate `SubjectRequestDto`/`SubjectResponseDto`).
3. Create repository in `Repository` package (`SubjectRepository extends JpaRepository<Subject, Long>`) — add custom query methods.
4. Create service interface in `Service` package (`SubjectService`).
5. Create implementation in `Implementation` package (`SubjectServiceImplementation`) — inject all needed repositories.
6. Add mapping methods and validation logic.
7. Create REST controller in `Controller` package (`SubjectController`).
8. Add CRUD endpoints and call only service interface from controller.
9. Test via Swagger UI: `http://localhost:7000/swagger-ui/index.html`.

## 8) Minimal Skeleton (Copy Pattern)

```java
@RestController
@RequestMapping("/api/subject")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectDto createSubject(@Valid @RequestBody SubjectDto requestDto) {
        return subjectService.createSubject(requestDto);
    }
}
```

Keep the same principle: Controller handles HTTP, Service defines contract, Implementation contains logic, Repository handles DB, DTOs are API payloads, Entity is table schema.
