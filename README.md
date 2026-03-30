# Student CRUD Practice (Spring Boot + JPA)

This project now includes a layered Student CRUD sample using your package structure:

- `Controller`: REST request/response handling
- `dto`: API payload fields only
- `Entity`: JPA table schema
- `Repository`: database access via Spring Data JPA
- `Service`: business contract (interface)
- `Implementation`: service logic implementation

## Feature Flow

1. Client sends HTTP request to `StudentController`.
2. `StudentController` validates payload and calls `StudentService`.
3. `StudentServiceImplementation` applies business rules and uses `StudentRepository`.
4. `StudentRepository` performs DB operations on `Student` entity.
5. Service maps `Student` to `StudentResponseDto` and returns to controller.
6. Controller returns JSON response to client.

## Student Endpoints

Base path: `/api/students`

- `POST /api/students` - create student
- `GET /api/students` - list all students
- `GET /api/students/{id}` - get one student
- `PUT /api/students/{id}` - update student
- `DELETE /api/students/{id}` - delete student

## DTO Samples

### StudentRequestDto

```json
{
  "firstName": "Andrew",
  "lastName": "Candare",
  "email": "andrew@example.com",
  "age": 22,
  "course": "Computer Science"
}
```

### StudentResponseDto

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

## Profiles

- Default profile: `h2` (set in `application.yml`)
- H2 config: `application-h2.yml`
- PostgreSQL config: `application-postgres.yml`

### Use H2 (default)

No extra setup needed. Data is in-memory and resets on app restart.

### Switch to PostgreSQL later

Use profile `postgres` and set environment variables if needed:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## Practice Ideas

1. Add pagination and sorting on `GET /api/students`.
2. Add search endpoints (`findByEmail`, `findByCourse`).
3. Add global exception handling under `Exceptions` package.
4. Add unit tests for service layer with mocks.
5. Add created/updated timestamps to entity.

## Request Flow Documentation

For a step-by-step request path (controller to DB and back), check:

- `docs/REQUEST_FLOW_GUIDE.md`

