# Spring Boot CRUD REST API - Complete Project Guide

**Project Name:** SampleSpringBootApplication  
**Architecture:** Layered (Controller → Service → Implementation → Repository → Entity)  
**Database:** H2 (in-memory) / PostgreSQL  
**Framework:** Spring Boot 4.0.5 with Spring Data JPA  
**Build Tool:** Gradle  
**Date:** March 30, 2026  
**Last Updated:** April 1, 2026

---

## TABLE OF CONTENTS

1. [Project Structure Overview](#project-structure-overview)
2. [System Architecture Flow](#system-architecture-flow)
3. [Controller Layer](#controller-layer)
4. [Service Layer](#service-layer)
5. [Entity & DTO Structure](#entity--dto-structure)
6. [Entity Relationships](#entity-relationships)
7. [Repository Layer](#repository-layer)
8. [Complete Data Flow](#complete-data-flow)
9. [API Endpoints Reference](#api-endpoints-reference)
10. [Annotations Reference](#annotations-reference)

---

## PROJECT STRUCTURE OVERVIEW

```
SampleSpringBootApplication/
├── src/main/java/com/codeWithJeff/SampleSpringBootApplication/
│   ├── Controller/
│   │   ├── StudentController.java          → /api/students
│   │   ├── TeacherController.java          → /api/teachers
│   │   ├── SubjectController.java          → /api/subject
│   │   └── SpringRestController.java       → /test
│   ├── Service/
│   │   ├── StudentService.java             (Interface/Contract)
│   │   ├── TeacherService.java             (Interface/Contract)
│   │   └── SubjectService.java             (Interface/Contract)
│   ├── Implementation/
│   │   ├── StudentServiceImplementation.java
│   │   ├── TeacherServiceImplementation.java
│   │   └── SubjectServiceImplementation.java
│   ├── Repository/
│   │   ├── StudentRepository.java          (JPA data access)
│   │   ├── TeacherRepository.java          (JPA data access)
│   │   └── SubjectRepository.java          (JPA data access)
│   ├── Entity/
│   │   ├── Student.java                    (DB table mapping)
│   │   ├── Teacher.java                    (DB table mapping)
│   │   └── Subject.java                    (DB table mapping — links Student & Teacher)
│   ├── dto/
│   │   ├── StudentRequestDto.java
│   │   ├── StudentResponseDto.java
│   │   ├── TeacherRequestDto.java
│   │   ├── TeacherResponseDto.java
│   │   └── SubjectDto.java                 (Single DTO for request & response)
│   ├── Exceptions/ (Reserved for error handling)
│   └── Util/ (Reserved for utilities)
├── src/main/resources/
│   ├── application.yml
│   ├── application-h2.yml
│   ├── application-postgres.yml
├── docs/
│   ├── COMPLETE_PROJECT_GUIDE.md
│   ├── REQUEST_FLOW_GUIDE.md
│   ├── AZURE_POSTGRES_SETUP.md
│   └── DOCUMENTATION_INDEX.md
└── build.gradle
```

---

## SYSTEM ARCHITECTURE FLOW

### Layered Architecture

```
┌──────────────────────────────┐
│      HTTP REQUEST            │
└────────────────┬─────────────┘
                 │
                 ▼
    ┌────────────────────────┐
    │  CONTROLLER LAYER      │
    │  @RestController       │
    │  - Handles HTTP        │
    │  - Validates input     │
    │  - Returns JSON        │
    └────────┬───────────────┘
             │
             ▼
    ┌────────────────────────┐
    │  SERVICE INTERFACE     │
    │  - Contract (What)     │
    │  - Loose coupling      │
    └────────┬───────────────┘
             │
             ▼
    ┌────────────────────────┐
    │  SERVICE IMPL          │
    │  @Service              │
    │  - Business logic      │
    │  - Validation logic    │
    │  - DTO ↔ Entity map    │
    │  - Error handling      │
    └────────┬───────────────┘
             │
             ▼
    ┌────────────────────────┐
    │  REPOSITORY            │
    │  JpaRepository         │
    │  - DB queries          │
    │  - Entity persistence  │
    └────────┬───────────────┘
             │
             ▼
    ┌────────────────────────┐
    │  DATABASE              │
    │  (H2/PostgreSQL)       │
    └────────────────────────┘
```

---

## CONTROLLER LAYER

### StudentController

**Location:** `Controller/StudentController.java`  
**Base URL:** `/api/students`

| Method | HTTP | Endpoint | Request | Response | Status |
|--------|------|----------|---------|----------|--------|
| `createStudent()` | POST | `/api/students` | StudentRequestDto | StudentResponseDto | 201 |
| `getAllStudents()` | GET | `/api/students` | None | List<StudentResponseDto> | 200 |
| `getStudentById()` | GET | `/api/students/{id}` | None | StudentResponseDto | 200 |
| `updateStudent()` | PUT | `/api/students/{id}` | StudentRequestDto | StudentResponseDto | 200 |
| `deleteStudent()` | DELETE | `/api/students/{id}` | None | None | 204 |

### TeacherController

**Location:** `Controller/TeacherController.java`  
**Base URL:** `/api/teachers`

| Method | HTTP | Endpoint | Request | Response | Status |
|--------|------|----------|---------|----------|--------|
| `createTeacher()` | POST | `/api/teachers` | TeacherRequestDto | TeacherResponseDto | 201 |
| `getAllTeachers()` | GET | `/api/teachers` | None | List<TeacherResponseDto> | 200 |
| `getTeacherById()` | GET | `/api/teachers/{id}` | None | TeacherResponseDto | 200 |
| `deleteTeacherById()` | DELETE | `/api/teachers/{id}` | None | None | 204 |

### SubjectController (NEW — April 1, 2026)

**Location:** `Controller/SubjectController.java`  
**Base URL:** `/api/subject`

| Method | HTTP | Endpoint | Request | Response | Status |
|--------|------|----------|---------|----------|--------|
| `createSubject()` | POST | `/api/subject` | SubjectDto | SubjectDto | 201 |

**Annotations:**
- `@RestController` → REST endpoint handler
- `@RequestMapping("/api/...")` → Base URL
- `@PostMapping/@GetMapping/@PutMapping/@DeleteMapping` → HTTP methods
- `@PathVariable Long id` → Extract {id} from URL
- `@RequestBody` → Parse JSON to DTO
- `@Valid` → Trigger DTO validation
- `@ResponseStatus()` → Set HTTP status

---

## SERVICE LAYER

### StudentService Interface

**Location:** `Service/StudentService.java`

```java
public interface StudentService {
    StudentResponseDto createStudent(StudentRequestDto requestDto);
    List<StudentResponseDto> getAllStudents();
    StudentResponseDto getStudentById(Long id);
    StudentResponseDto updateStudent(Long id, StudentRequestDto requestDto);
    void deleteStudent(Long id);
}
```

### StudentServiceImplementation - Essential Methods

| Method | Purpose | Key Logic |
|--------|---------|-----------|
| `createStudent()` | CREATE | Validate email uniqueness → Build entity → Save → Map to DTO |
| `getAllStudents()` | READ ALL | Fetch all → Map each → Return list |
| `getStudentById()` | READ ONE | Find by ID (throw 404 if not found) → Map to DTO |
| `updateStudent()` | UPDATE | Find by ID → Validate new data → Update fields → Save → Map |
| `deleteStudent()` | DELETE | Find by ID (throw 404 if not found) → Delete |
| `toResponse()` | HELPER | Map Student entity → StudentResponseDto |

### TeacherService Interface

```java
public interface TeacherService {
    TeacherResponseDto createTeacher(TeacherRequestDto requestDto);
    List<TeacherResponseDto> getAllTeachers();
    TeacherResponseDto getTeacherById(Long id);
    void deleteTeacherById(Long id);
}
```

### TeacherServiceImplementation - Essential Methods

| Method | Purpose | Key Logic |
|--------|---------|-----------|
| `createTeacher()` | CREATE | Build entity (firstName, lastName only) → Save → Map to DTO |
| `getAllTeachers()` | READ ALL | Fetch all → Map each → Return list |
| `getTeacherById()` | READ ONE | Find by ID (throw 404 if not found) → Map to DTO |
| `deleteTeacherById()` | DELETE | Find by ID (throw 404 if not found) → Delete |
| `toResponseTeacher()` | HELPER | Map Teacher entity → TeacherResponseDto |

> **Note:** Teacher no longer has a `course` field. Subjects are assigned separately via the Subject endpoint.

### SubjectService Interface (NEW — April 1, 2026)

```java
public interface SubjectService {
    SubjectDto createSubject(SubjectDto requestSubjectDto);
}
```

### SubjectServiceImplementation - Essential Methods

| Method | Purpose | Key Logic |
|--------|---------|-----------|
| `createSubject()` | CREATE | Validate subject name uniqueness → Validate teacher not already assigned → Look up Teacher & Student → Build Subject entity → Save → Map to DTO |

**Validation Flow in `createSubject()`:**

```
1. existsBySubject(name)         → 409 CONFLICT "Subject already exists"
2. existsByTeacher_TeacherId(id) → 409 CONFLICT "Teacher already has a subject assigned"
3. findById(teacherId)           → 404 NOT_FOUND "Teacher not found"
4. findById(studentId)           → 404 NOT_FOUND "Student not found"
5. Build Subject with student, teacher, subject name
6. Save and return response with names resolved
```

**Implementation Pattern (Subject — 2-step save with relationships):**
```java
@Service
@RequiredArgsConstructor
public class SubjectServiceImplementation implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Override
    public SubjectDto createSubject(SubjectDto requestSubjectDto) {
        // 1. Validate subject name doesn't exist
        if (subjectRepository.existsBySubject(requestSubjectDto.getSubject())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject already exists");
        }
        // 2. Validate teacher doesn't already have a subject (OneToOne)
        if (subjectRepository.existsByTeacher_TeacherId(requestSubjectDto.getTeacherId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Teacher already has a subject assigned");
        }
        // 3. Look up Teacher and Student entities
        Teacher teacher = teacherRepository.findById(requestSubjectDto.getTeacherId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        Student student = studentRepository.findById(requestSubjectDto.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        // 4. Build and save Subject entity
        Subject subject = Subject.builder()
                .student(student)
                .teacher(teacher)
                .subject(requestSubjectDto.getSubject())
                .build();
        Subject saved = subjectRepository.save(subject);

        // 5. Return response with resolved names
        return SubjectDto.builder()
                .subjectId(saved.getSubjectId())
                .studentId(student.getStudentId())
                .teacherId(teacher.getTeacherId())
                .subject(saved.getSubject())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .teacherName(teacher.getFirstName() + " " + teacher.getLastName())
                .build();
    }
}
```

---

## ENTITY & DTO STRUCTURE

### Student Entity

**Location:** `Entity/Student.java`

```java
@Entity
@Table(name = "students")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, length = 100)
    private String course;
}
```

**Generated Table:**
```sql
CREATE TABLE students (
    student_id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    age INTEGER NOT NULL,
    course VARCHAR(100) NOT NULL
);
```

### Teacher Entity (UPDATED — April 1, 2026)

**Location:** `Entity/Teacher.java`  
> **Changed:** Removed `course` field. Subject assignment is now handled by the `Subject` entity.

```java
@Entity
@Table(name = "teachers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long teacherId;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;
}
```

**Generated Table:**
```sql
CREATE TABLE teachers (
    teacher_id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL
);
```

### Subject Entity (NEW — April 1, 2026)

**Location:** `Entity/Subject.java`  
> Links Students and Teachers together. One teacher can teach one subject (`@OneToOne`). Many students can take one subject (`@ManyToOne`).

```java
@Entity
@Table(name = "Subject")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long subjectId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @OneToOne
    @JoinColumn(name = "teacher_id", nullable = false, unique = true)
    private Teacher teacher;

    @Column(nullable = false, length = 100)
    private String subject;
}
```

**Generated Table:**
```sql
CREATE TABLE subject (
    subject_id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    teacher_id BIGINT NOT NULL UNIQUE REFERENCES teachers(teacher_id),
    subject VARCHAR(100) NOT NULL
);
```

### DTO Structure

#### StudentRequestDto (API Input — no `id`)
```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentRequestDto {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email @NotBlank private String email;
    @Min(16) @Max(120) private Integer age;
    @NotBlank private String course;
}
```

#### StudentResponseDto (API Output — includes `id`)
```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private String course;
}
```

#### TeacherRequestDto (UPDATED — no `course`)
```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherRequestDto {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
}
```

#### TeacherResponseDto (UPDATED — no `course`)
```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
}
```

#### SubjectDto (NEW — Single DTO for request & response)
```java
@Data @AllArgsConstructor @RequiredArgsConstructor @Builder
public class SubjectDto {
    private Long subjectId;          // Response only (auto-generated)

    @NotNull private Long studentId;  // Request: which student
    @NotNull private Long teacherId;  // Request: which teacher

    @NotBlank private String subject; // Request & Response: subject name

    private String studentName;       // Response only: resolved name
    private String teacherName;       // Response only: resolved name
}
```

**Request body (what client sends):**
```json
{
  "studentId": 1,
  "teacherId": 2,
  "subject": "Mathematics"
}
```

**Response body (what API returns):**
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

> **Note:** `subjectId`, `studentName`, `teacherName` are ignored on request — they are populated by the server in the response.

### Entity ↔ DTO Mapping

**In Service Implementation:**

```java
// DTO → Entity (Student create)
Student student = Student.builder()
    .firstName(requestDto.getFirstName())
    .lastName(requestDto.getLastName())
    .email(requestDto.getEmail())
    .age(requestDto.getAge())
    .course(requestDto.getCourse())
    .build();

// Entity → DTO (Student response)
return StudentResponseDto.builder()
    .id(student.getStudentId())
    .firstName(student.getFirstName())
    .lastName(student.getLastName())
    .email(student.getEmail())
    .age(student.getAge())
    .course(student.getCourse())
    .build();

// DTO → Entity (Subject create — requires lookups first)
Subject subject = Subject.builder()
    .student(studentEntity)    // fetched from DB by studentId
    .teacher(teacherEntity)    // fetched from DB by teacherId
    .subject(requestDto.getSubject())
    .build();
```

---

## ENTITY RELATIONSHIPS (NEW — April 1, 2026)

### Relationship Diagram

```
┌────────────┐       ┌────────────┐       ┌────────────┐
│  students  │       │  subject   │       │  teachers  │
├────────────┤       ├────────────┤       ├────────────┤
│ student_id │◄──┐   │ subject_id │   ┌──►│ teacher_id │
│ first_name │   └───│ student_id │   │   │ first_name │
│ last_name  │       │ teacher_id │───┘   │ last_name  │
│ email      │       │ subject    │       └────────────┘
│ age        │       └────────────┘
│ course     │
└────────────┘
```

### Relationship Types

| Relationship | Type | Annotation | Meaning |
|---|---|---|---|
| Subject → Student | `@ManyToOne` | `@JoinColumn(name="student_id")` | Many subjects can reference one student |
| Subject → Teacher | `@OneToOne` | `@JoinColumn(name="teacher_id", unique=true)` | Each teacher teaches exactly one subject |

### Why Relationships?

- **`@ManyToOne` (Subject → Student):** A student can be enrolled in many subjects. Each subject row references one student via `student_id` foreign key.
- **`@OneToOne` (Subject → Teacher):** Each teacher teaches exactly one subject. The `unique = true` constraint on `teacher_id` ensures no two subjects share the same teacher.
- **Foreign keys are defined in the Subject entity** (the "owning side"). Student and Teacher entities don't need to know about Subject.

### Key JPA Relationship Annotations

| Annotation | Purpose |
|---|---|
| `@ManyToOne` | Many rows of this entity can reference one row of the target entity |
| `@OneToOne` | One row of this entity maps to exactly one row of the target entity |
| `@JoinColumn(name="...")` | Specifies the FK column name in the database |
| `nullable = false` | FK cannot be null — the relationship is required |
| `unique = true` | FK must be unique — enforces one-to-one at DB level |

---

## REPOSITORY LAYER

### StudentRepository

**Location:** `Repository/StudentRepository.java`

```java
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);
}
```

### TeacherRepository

```java
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    // Uses only inherited JpaRepository methods
}
```

### SubjectRepository (NEW — April 1, 2026)

```java
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    boolean existsBySubject(String subject);              // Check if subject name exists
    boolean existsByTeacher_TeacherId(Long teacherId);    // Check if teacher already assigned

    @Override
    List<Subject> findAll(Sort sort);
}
```

**Spring Data JPA Derived Query Naming:**
- `existsBySubject(String)` → `SELECT COUNT(*) > 0 FROM subject WHERE subject = ?`
- `existsByTeacher_TeacherId(Long)` → `SELECT COUNT(*) > 0 FROM subject WHERE teacher_id = ?`
- The `_` in `Teacher_TeacherId` means: navigate to `teacher` field, then access `teacherId` property

> **Important:** Method names must exactly match field names. `existsBySubject` (with **s** after `exists`) — not `existBySubject`. `TeacherId` (lowercase **d**) — not `TeacherID`.

**Inherited from JpaRepository:**
```java
studentRepository.findByEmail(email)       // Optional<Student>
studentRepository.findById(id)             // Optional<Student>
studentRepository.findAll()                // List<Student>
studentRepository.save(student)            // Saved entity with ID
studentRepository.delete(student)          // DELETE
subjectRepository.existsBySubject(name)    // boolean
subjectRepository.existsByTeacher_TeacherId(id) // boolean
```

---

## COMPLETE DATA FLOW

### CREATE Student Flow (POST /api/students)

```
1. CLIENT REQUEST
   POST /api/students
   Content-Type: application/json
   {
     "firstName": "John",
     "lastName": "Doe",
     "email": "john@example.com",
     "age": 20,
     "course": "Math"
   }

2. CONTROLLER: StudentController.createStudent()
   - @Valid triggers DTO validation
   - Calls studentService.createStudent(requestDto)

3. SERVICE: StudentServiceImplementation.createStudent()
   - Call: studentRepository.findByEmail("john@example.com")
   - If found: throw 409 Conflict
   - Build Student entity
   - Call: studentRepository.save(student)

4. REPOSITORY & DATABASE
   - SQL: INSERT INTO students (first_name, last_name, ...)
   - DB generates student_id = 1
   - Returns Student(studentId=1, ...)

5. SERVICE: toResponse(student)
   - Map Student → StudentResponseDto
   - Returns response with id

6. CONTROLLER RESPONSE (201 Created)
   {
     "id": 1,
     "firstName": "John",
     "lastName": "Doe",
     "email": "john@example.com",
     "age": 20,
     "course": "Math"
   }
```

### CREATE Subject Flow (POST /api/subject) — NEW

```
1. CLIENT REQUEST
   POST /api/subject
   Content-Type: application/json
   {
     "studentId": 1,
     "teacherId": 2,
     "subject": "Mathematics"
   }

2. CONTROLLER: SubjectController.createSubject()
   - @Valid triggers DTO validation
   - Calls subjectService.createSubject(requestDto)

3. SERVICE: SubjectServiceImplementation.createSubject()
   - Validate: existsBySubject("Mathematics") → if true: throw 409
   - Validate: existsByTeacher_TeacherId(2) → if true: throw 409
   - Lookup: teacherRepository.findById(2) → if empty: throw 404
   - Lookup: studentRepository.findById(1) → if empty: throw 404
   - Build Subject entity with Student + Teacher + subject name
   - Call: subjectRepository.save(subject)

4. REPOSITORY & DATABASE
   - SQL: INSERT INTO subject (student_id, teacher_id, subject) VALUES (1, 2, 'Mathematics')
   - DB generates subject_id = 1

5. SERVICE: Build SubjectDto response
   - Resolve studentName from Student entity
   - Resolve teacherName from Teacher entity

6. CONTROLLER RESPONSE (201 Created)
   {
     "subjectId": 1,
     "studentId": 1,
     "teacherId": 2,
     "subject": "Mathematics",
     "studentName": "John Doe",
     "teacherName": "Jane Smith"
   }
```

---

## API ENDPOINTS REFERENCE

### Student Endpoints

#### 1. Create Student (POST)
```http
POST /api/students HTTP/1.1
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "age": 20,
  "course": "Math"
}
```
**Response: 201 Created**

#### 2. List All Students (GET)
```http
GET /api/students HTTP/1.1
```
**Response: 200 OK** → List of StudentResponseDto

#### 3. Get Student by ID (GET)
```http
GET /api/students/1 HTTP/1.1
```
**Response: 200 OK** → Single StudentResponseDto  
**Error: 404 Not Found** → "Student not found"

#### 4. Update Student (PUT)
```http
PUT /api/students/1 HTTP/1.1
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane@example.com",
  "age": 21,
  "course": "Physics"
}
```
**Response: 200 OK**  
**Error: 409 Conflict** → "Email already exists"

#### 5. Delete Student (DELETE)
```http
DELETE /api/students/1 HTTP/1.1
```
**Response: 204 No Content**

### Teacher Endpoints

#### 1. Create Teacher (POST)
```http
POST /api/teachers HTTP/1.1
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith"
}
```
**Response: 201 Created**

#### 2. List All Teachers (GET)
```http
GET /api/teachers HTTP/1.1
```
**Response: 200 OK** → List of TeacherResponseDto

#### 3. Get Teacher by ID (GET)
```http
GET /api/teachers/1 HTTP/1.1
```
**Response: 200 OK**  
**Error: 404 Not Found** → "Teacher not found"

#### 4. Delete Teacher (DELETE)
```http
DELETE /api/teachers/1 HTTP/1.1
```
**Response: 204 No Content**

### Subject Endpoints (NEW — April 1, 2026)

#### 1. Create Subject (POST)
```http
POST /api/subject HTTP/1.1
Content-Type: application/json

{
  "studentId": 1,
  "teacherId": 2,
  "subject": "Mathematics"
}
```
**Response: 201 Created**
```json
{
  "subjectId": 1,
  "studentId": 1,
  "teacherId": 2,
  "subject": "Mathematics",
  "studentName": "John Doe",
  "teacherName": "Jane Smith"
}
```
**Error: 409 Conflict** → "Subject already exists" or "Teacher already has a subject assigned"  
**Error: 404 Not Found** → "Teacher not found" or "Student not found"

### Swagger UI

Visit **`http://localhost:7000/swagger-ui/index.html`** to test all endpoints interactively.

---

## ANNOTATIONS REFERENCE

### Spring Web Annotations
| Annotation | Purpose |
|---|---|
| `@RestController` | REST endpoint handler (returns JSON) |
| `@RequestMapping("/path")` | Base URL path |
| `@PostMapping` | Handle POST requests |
| `@GetMapping` | Handle GET requests |
| `@PutMapping` | Handle PUT requests |
| `@DeleteMapping` | Handle DELETE requests |
| `@PathVariable` | Extract URL path variable |
| `@RequestBody` | Parse JSON body to DTO |
| `@ResponseStatus(...)` | Set HTTP response status |
| `@Valid` | Trigger DTO validation |

### Spring Bean Annotations
| Annotation | Purpose |
|---|---|
| `@Service` | Service component bean |
| `@Repository` | Repository component bean |
| `@RequiredArgsConstructor` | Generate constructor for final fields |

### JPA/Persistence Annotations
| Annotation | Purpose |
|---|---|
| `@Entity` | JPA entity (maps to DB table) |
| `@Table(name="...")` | Specify table name |
| `@Id` | Primary key field |
| `@GeneratedValue(GenerationType.IDENTITY)` | Auto-increment primary key |
| `@Column(...)` | Column constraints (nullable, unique, length) |
| `@ManyToOne` | Many-to-one relationship (FK on this side) |
| `@OneToOne` | One-to-one relationship |
| `@JoinColumn(name="...")` | FK column name + constraints |

### Validation Annotations
| Annotation | Purpose |
|---|---|
| `@NotBlank` | Cannot be null or empty (Strings) |
| `@NotNull` | Cannot be null (any type) |
| `@Email` | Valid email format |
| `@Min(value)` | Minimum numeric value |
| `@Max(value)` | Maximum numeric value |

### Lombok Annotations
| Annotation | Purpose |
|---|---|
| `@Data` | Generate getters, setters, toString, equals, hashCode |
| `@NoArgsConstructor` | No-argument constructor |
| `@AllArgsConstructor` | All-argument constructor |
| `@RequiredArgsConstructor` | Constructor for final fields |
| `@Builder` | Builder pattern support |

---

## RUNNING THE APPLICATION

### Start the App
```bash
.\gradlew.bat bootRun
```

### Test Endpoints (PowerShell)
```powershell
# Create student
$student = @{
  firstName = "Andrew"
  lastName = "Candare"
  email = "andrew@example.com"
  age = 22
  course = "Computer Science"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:7000/api/students" `
  -ContentType "application/json" -Body $student

# Create teacher
$teacher = @{
  firstName = "Jane"
  lastName = "Smith"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:7000/api/teachers" `
  -ContentType "application/json" -Body $teacher

# Create subject (link student 1 to teacher 1)
$subject = @{
  studentId = 1
  teacherId = 1
  subject = "Mathematics"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:7000/api/subject" `
  -ContentType "application/json" -Body $subject

# Get all
Invoke-RestMethod -Uri "http://localhost:7000/api/students"
Invoke-RestMethod -Uri "http://localhost:7000/api/teachers"
```

### UI Tools
- **Swagger UI:** `http://localhost:7000/swagger-ui/index.html`
- **H2 Console:** `http://localhost:7000/h2-console`
  - JDBC URL: `jdbc:h2:mem:studentdb`
  - User: `sa`
  - Password: (leave blank)

---

## KEY DESIGN PRINCIPLES

1. **Layered Architecture** → Separation of concerns
2. **Service Interface** → Loose coupling
3. **DTO Mapping** → API shape independent of DB schema
4. **Repository Pattern** → Database abstraction
5. **Entity Relationships** → Relational data defined in JPA, not raw SQL
6. **Validation** → Multiple levels (DTO, Service)
7. **Error Handling** → Appropriate HTTP status codes (404, 409)
8. **Annotations** → Declarative Spring configuration

---

**Document Version:** 2.0  
**Last Updated:** April 1, 2026  
**Changes in v2.0:** Added Subject entity with relationships (@ManyToOne, @OneToOne), SubjectDto, SubjectService, SubjectServiceImplementation, SubjectController, SubjectRepository. Updated Teacher entity (removed course field). Added Entity Relationships section.  
**Next Steps:** Add GET/DELETE subject endpoints, separate SubjectRequestDto/SubjectResponseDto, add pagination, global exception handlers, unit tests
