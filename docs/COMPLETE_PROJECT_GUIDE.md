# Spring Boot CRUD REST API - Complete Project Guide

**Project Name:** SampleSpringBootApplication  
**Architecture:** Layered (Controller → Service → Implementation → Repository → Entity)  
**Database:** H2 (in-memory) / PostgreSQL  
**Framework:** Spring Boot 4.0.5 with Spring Data JPA  
**Build Tool:** Gradle  
**Date:** March 30, 2026

---

## TABLE OF CONTENTS

1. [Project Structure Overview](#project-structure-overview)
2. [System Architecture Flow](#system-architecture-flow)
3. [Controller Layer](#controller-layer)
4. [Service Layer](#service-layer)
5. [Entity & DTO Structure](#entity--dto-structure)
6. [Repository Layer](#repository-layer)
7. [Complete Data Flow](#complete-data-flow)
8. [API Endpoints Reference](#api-endpoints-reference)
9. [Annotations Reference](#annotations-reference)

---

## PROJECT STRUCTURE OVERVIEW

```
SampleSpringBootApplication/
├── src/main/java/com/codeWithJeff/SampleSpringBootApplication/
│   ├── Controller/
│   │   ├── StudentController.java          → /api/students
│   │   ├── TeacherController.java          → /api/teachers
│   │   └── SpringRestController.java       → /test
│   ├── Service/
│   │   ├── StudentService.java             (Interface/Contract)
│   │   └── TeacherService.java             (Interface/Contract)
│   ├── Implementation/
│   │   ├── StudentServiceImplementation.java
│   │   └── TeacherServiceImplementation.java
│   ├── Repository/
│   │   ├── StudentRepository.java          (JPA data access)
│   │   └── TeacherRepository.java          (JPA data access)
│   ├── Entity/
│   │   ├── Student.java                    (DB table mapping)
│   │   └── Teacher.java                    (DB table mapping)
│   ├── dto/
│   │   ├── StudentRequestDto.java
│   │   ├── StudentResponseDto.java
│   │   ├── TeacherRequestDto.java
│   │   └── TeacherResponseDto.java
│   ├── Exceptions/ (Reserved for error handling)
│   └── Util/ (Reserved for utilities)
├── src/main/resources/
│   ├── application.yml
│   ├── application-h2.yml
│   ├── application-postgres.yml
├── docs/
│   ├── COMPLETE_PROJECT_GUIDE.md
│   ├── REQUEST_FLOW_GUIDE.md
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

**Annotations:**
- `@RestController` → REST endpoint handler
- `@RequestMapping("/api/students")` → Base URL
- `@PostMapping/@GetMapping/@PutMapping/@DeleteMapping` → HTTP methods
- `@PathVariable Long id` → Extract {id} from URL
- `@RequestBody` → Parse JSON to DTO
- `@Valid` → Trigger DTO validation
- `@ResponseStatus()` → Set HTTP status

### TeacherController

**Location:** `Controller/TeacherController.java`  
**Base URL:** `/api/teachers`

| Method | HTTP | Endpoint | Request | Response | Status |
|--------|------|----------|---------|----------|--------|
| `createTeacher()` | POST | `/api/teachers` | TeacherRequestDto | TeacherResponseDto | 201 |
| `getAllTeachers()` | GET | `/api/teachers` | None | List<TeacherResponseDto> | 200 |
| `getTeacherById()` | GET | `/api/teachers/{id}` | None | TeacherResponseDto | 200 |
| `deleteTeacherById()` | DELETE | `/api/teachers/{id}` | None | None | 204 |

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

**Implementation Pattern:**
```java
@Service
@RequiredArgsConstructor
public class StudentServiceImplementation implements StudentService {
    private final StudentRepository studentRepository;
    
    @Override
    public StudentResponseDto createStudent(StudentRequestDto requestDto) {
        // Validation
        studentRepository.findByEmail(requestDto.getEmail())
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Email already exists");
            });
        
        // Build entity from DTO
        Student student = Student.builder()
            .firstName(requestDto.getFirstName())
            .lastName(requestDto.getLastName())
            .email(requestDto.getEmail())
            .age(requestDto.getAge())
            .course(requestDto.getCourse())
            .build();
        
        // Save and map
        return toResponse(studentRepository.save(student));
    }
    
    @Override
    public List<StudentResponseDto> getAllStudents() {
        return studentRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }
    
    @Override
    public StudentResponseDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Student not found"));
        return toResponse(student);
    }
    
    @Override
    public StudentResponseDto updateStudent(Long id, StudentRequestDto requestDto) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Student not found"));
        
        studentRepository.findByEmail(requestDto.getEmail())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Email already exists");
            });
        
        student.setFirstName(requestDto.getFirstName());
        student.setLastName(requestDto.getLastName());
        student.setEmail(requestDto.getEmail());
        student.setAge(requestDto.getAge());
        student.setCourse(requestDto.getCourse());
        
        return toResponse(studentRepository.save(student));
    }
    
    @Override
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Student not found"));
        studentRepository.delete(student);
    }
    
    private StudentResponseDto toResponse(Student student) {
        return StudentResponseDto.builder()
            .id(student.getId())
            .firstName(student.getFirstName())
            .lastName(student.getLastName())
            .email(student.getEmail())
            .age(student.getAge())
            .course(student.getCourse())
            .build();
    }
}
```

### TeacherService Interface

```java
public interface TeacherService {
    TeacherResponseDto createTeacher(TeacherRequestDto requestDto);
    List<TeacherResponseDto> getAllTeachers();
    TeacherResponseDto getTeacherById(Long id);
    void deleteTeacherById(Long id);
}
```

---

## ENTITY & DTO STRUCTURE

### Student Entity

**Location:** `Entity/Student.java`

```java
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                           // Auto-increment PK
    
    @Column(nullable = false, length = 100)
    private String firstName;
    
    @Column(nullable = false, length = 100)
    private String lastName;
    
    @Column(nullable = false, unique = true, length = 150)
    private String email;                      // UNIQUE constraint
    
    @Column(nullable = false)
    private Integer age;
    
    @Column(nullable = false, length = 100)
    private String course;
}
```

**Generated Table:**
```sql
CREATE TABLE students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    age INTEGER NOT NULL,
    course VARCHAR(100) NOT NULL
);
```

### StudentRequestDto (API Input)

**No `id` field** (DB generates it)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRequestDto {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email @NotBlank private String email;
    @Min(16) @Max(120) private Integer age;
    @NotBlank private String course;
}
```

### StudentResponseDto (API Output)

**Includes `id` field**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private String course;
}
```

### Teacher Entity & DTOs

**Entity:**
```java
@Entity
@Table(name = "teachers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Teacher {
    @Id @GeneratedValue(GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String firstName;
    
    @Column(nullable = false, length = 100)
    private String lastName;
    
    @Column(nullable = false, length = 100)
    private String course;
}
```

**Request DTO (no id):**
```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherRequestDto {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank private String course;
}
```

**Response DTO (includes id):**
```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String course;
}
```

### Entity ↔ DTO Mapping

**In Service Implementation:**

```java
// DTO → Entity (in create methods)
Student student = Student.builder()
    .firstName(requestDto.getFirstName())
    .lastName(requestDto.getLastName())
    .email(requestDto.getEmail())
    .age(requestDto.getAge())
    .course(requestDto.getCourse())
    .build();

// Entity → DTO (in response methods)
return StudentResponseDto.builder()
    .id(student.getId())
    .firstName(student.getFirstName())
    .lastName(student.getLastName())
    .email(student.getEmail())
    .age(student.getAge())
    .course(student.getCourse())
    .build();
```

---

## REPOSITORY LAYER

### StudentRepository

**Location:** `Repository/StudentRepository.java`

```java
public interface StudentRepository extends JpaRepository<Student, Long> {
    // Inherited from JpaRepository:
    // - save(Student) → INSERT/UPDATE
    // - findById(Long) → SELECT by ID
    // - findAll() → SELECT all
    // - delete(Student) → DELETE
    // - deleteById(Long) → DELETE by ID
    // - existsById(Long) → Check if exists
    
    // Custom query method
    Optional<Student> findByEmail(String email);
}
```

**Usage in Service:**
```java
studentRepository.findByEmail(email)       // Optional<Student>
studentRepository.findById(id)             // Optional<Student>
studentRepository.findAll()                // List<Student>
studentRepository.save(student)            // Saved entity with ID
studentRepository.delete(student)          // DELETE
```

### TeacherRepository

```java
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    List<Teacher> findByCourse(String course);
}
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
   - DB generates id = 1
   - Returns Student(id=1, ...)

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

### READ Student by ID Flow (GET /api/students/1)

```
1. CLIENT: GET /api/students/1

2. CONTROLLER: getStudentById(@PathVariable Long id=1)
   - Calls studentService.getStudentById(1)

3. SERVICE: StudentServiceImplementation.getStudentById(1)
   - Call: studentRepository.findById(1)
   - If empty: throw 404
   - If found: map to DTO

4. REPOSITORY & DATABASE
   - SQL: SELECT * FROM students WHERE id = 1
   - Returns Optional<Student>

5. SERVICE: toResponse(student)
   - Maps entity to DTO

6. CONTROLLER RESPONSE (200 OK)
   {
     "id": 1,
     "firstName": "John",
     ...
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

Similar pattern to Student (POST, GET list, GET by id, DELETE)

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

### Validation Annotations
| Annotation | Purpose |
|---|---|
| `@NotBlank` | Cannot be null or empty |
| `@Email` | Valid email format |
| `@Min(value)` | Minimum numeric value |
| `@Max(value)` | Maximum numeric value |

### Lombok Annotations
| Annotation | Purpose |
|---|---|
| `@Data` | Generate getters, setters, toString, equals, hashCode |
| `@NoArgsConstructor` | No-argument constructor |
| `@AllArgsConstructor` | All-argument constructor |
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

# Get all
Invoke-RestMethod -Uri "http://localhost:7000/api/students"

# Get by ID
Invoke-RestMethod -Uri "http://localhost:7000/api/students/1"
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
5. **Validation** → Multiple levels (DTO, Service)
6. **Error Handling** → Appropriate HTTP status codes
7. **Annotations** → Declarative Spring configuration

---

**Document Version:** 1.0  
**Last Updated:** March 30, 2026  
**Next Steps:** Add pagination, filtering, global exception handlers, unit tests

