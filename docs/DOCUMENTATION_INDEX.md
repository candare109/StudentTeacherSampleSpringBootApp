
# Documentation Index

**Last Updated:** April 1, 2026

## Available Documentation

- `docs/COMPLETE_PROJECT_GUIDE.md`
  - Full project structure, layered architecture, DTO/entity/repository/service/controller relationships, entity relationships (@ManyToOne, @OneToOne), and endpoint reference.

- `docs/REQUEST_FLOW_GUIDE.md`
  - Step-by-step request flow using `GET /api/students/{id}` and `POST /api/subject`.

- `docs/AZURE_POSTGRES_SETUP.md`
  - Azure Database for PostgreSQL Flexible Server integration steps, environment variables, psql commands, and troubleshooting.

- `docs/AZURE_KEYVAULT_SETUP.md`
  - Step-by-step Azure Key Vault creation (every portal tab explained), storing secrets, granting access, Spring Boot integration, and troubleshooting.

- `docs/JWT_AUTHENTICATION_GUIDE.md`
  - Step-by-step JWT authentication guide — how JWT works, full implementation code, testing with Postman, and request flow diagrams.

- `README.md`
  - Quick start, endpoints, profiles, Azure run examples, psql commands, git guide, and troubleshooting.

## Suggested Reading Order

1. `README.md`
2. `docs/COMPLETE_PROJECT_GUIDE.md`
3. `docs/REQUEST_FLOW_GUIDE.md`
4. `docs/AZURE_POSTGRES_SETUP.md`
5. `docs/AZURE_KEYVAULT_SETUP.md`
6. `docs/JWT_AUTHENTICATION_GUIDE.md`

## Current Project State (April 1, 2026)

### Entities
| Entity | Table | Fields |
|---|---|---|
| Student | `students` | studentId, firstName, lastName, email, age, course |
| Teacher | `teachers` | teacherId, firstName, lastName |
| Subject | `subject` | subjectId, student (FK), teacher (FK), subject |

### Entity Relationships
| Relationship | Type | Constraint |
|---|---|---|
| Subject → Student | `@ManyToOne` | Many subjects can reference one student |
| Subject → Teacher | `@OneToOne` | Each teacher teaches exactly one subject |

### API Endpoints
| Resource | Base URL | Operations |
|---|---|---|
| Students | `/api/students` | POST, GET all, GET by ID, PUT, DELETE |
| Teachers | `/api/teachers` | POST, GET all, GET by ID, DELETE |
| Subjects | `/api/subject` | POST |

## Current Profile Setup

- Default active profile: `h2` (set via `SPRING_PROFILES_ACTIVE` env var)
- Local H2 profile config: `src/main/resources/application-h2.yml`
- Azure/local PostgreSQL profile config: `src/main/resources/application-postgres.yml`

## Running With Azure PostgreSQL

Use either:

```powershell
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://<server-name>.postgres.database.azure.com:5432/studentdb?sslmode=require"
$env:DB_USERNAME="<admin-username>"
$env:DB_PASSWORD="<admin-password>"
.\gradlew.bat bootRun
```

Or the helper script:

```powershell
.\scripts\run-azure-postgres.ps1 -ServerName "<server-name>" -DbUsername "<admin-username>" -DbPassword "<admin-password>"
```

## Azure CLI — Quick Database Check

```bash
# List tables
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "\dt"

# Check table structure
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "\d subject"

# Query data
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "SELECT * FROM subject;"
```
