# 🎓 SampleSpringBootApplication

A **Spring Boot 4.0.5** REST API for managing Students and Teachers, built with a clean layered architecture. Supports both **H2 (in-memory)** for local development and **Azure PostgreSQL** for cloud deployment.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Azure-blue)
![Gradle](https://img.shields.io/badge/Build-Gradle-02303A)

---

## 📑 Table of Contents

- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Running with H2 (Local)](#-running-with-h2-local-default)
- [Running with Azure PostgreSQL](#️-running-with-azure-postgresql)
- [Azure Cloud Shell — psql Commands](#-azure-cloud-shell--psql-commands)
- [Azure Firewall Setup](#-azure-firewall-setup)
- [API Endpoints](#-api-endpoints)
- [Database Schema](#️-database-schema)
- [Request Flow](#-request-flow)
- [Configuration Files](#️-configuration-files)
- [Git Commands Guide](#-git-commands-guide)
- [Troubleshooting](#-troubleshooting)
- [Practice Ideas](#-practice-ideas)
- [Documentation](#-documentation)

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| ORM | Spring Data JPA + Hibernate |
| Local DB | H2 (in-memory, PostgreSQL mode) |
| Cloud DB | Azure Database for PostgreSQL Flexible Server |
| Build | Gradle (Groovy DSL) |
| API Docs | SpringDoc OpenAPI 3.0.2 (Swagger UI) |
| Utilities | Lombok |
| Testing | JUnit 5 + Spring Boot Test |

---

## 📁 Project Structure

```
src/main/java/com/codeWithJeff/SampleSpringBootApplication/
├── SampleSpringBootApplication.java      # @SpringBootApplication entry point
├── Controller/
│   ├── StudentController.java            # /api/students REST endpoints
│   ├── TeacherController.java            # /api/teachers REST endpoints
│   └── SpringRestController.java
├── dto/
│   ├── StudentRequestDto.java            # Student input validation
│   ├── StudentResponseDto.java           # Student API response
│   ├── TeacherRequestDto.java            # Teacher input validation
│   └── TeacherResponseDto.java           # Teacher API response
├── Entity/
│   ├── Student.java                      # JPA entity → students table
│   └── Teacher.java                      # JPA entity → teachers table
├── Repository/
│   ├── StudentRepository.java            # JpaRepository<Student, Long>
│   └── TeacherRepository.java            # JpaRepository<Teacher, Long>
├── Service/
│   ├── StudentService.java               # Business contract (interface)
│   └── TeacherService.java               # Business contract (interface)
├── Implementation/
│   ├── StudentServiceImplementation.java # Service logic
│   └── TeacherServiceImplementation.java # Service logic
├── Exceptions/                           # Custom exception handling
└── Util/                                 # Utility classes

src/main/resources/
├── application.yml                       # Profile selector (default: h2)
├── application-h2.yml                    # H2 in-memory config
└── application-postgres.yml              # Azure PostgreSQL config (env vars)

scripts/
└── run-azure-postgres.ps1                # PowerShell helper to launch with Azure DB

docs/
├── AZURE_POSTGRES_SETUP.md
├── COMPLETE_PROJECT_GUIDE.md
├── DOCUMENTATION_INDEX.md
└── REQUEST_FLOW_GUIDE.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** (or JDK 17+ configured in `gradle.properties`)
- **Git**
- **Azure account** (only if using PostgreSQL)

### Clone the repository

```bash
git clone https://github.com/<your-username>/SampleSpringBootApplication.git
cd SampleSpringBootApplication
```

---

## 💻 Running with H2 (Local, Default)

No configuration needed — H2 is the default profile.

```powershell
.\gradlew.bat bootRun
```

| Resource | URL |
|---|---|
| API Base | `http://localhost:7000/api/students` |
| Swagger UI | `http://localhost:7000/swagger-ui/index.html` |
| H2 Console | `http://localhost:7000/h2-console` |

**H2 Console settings:**

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:studentdb` |
| User | `sa` |
| Password | *(empty)* |

---

## ☁️ Running with Azure PostgreSQL

### Option 1: Set environment variables manually (PowerShell)

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DB_URL      = "jdbc:postgresql://<server-name>.postgres.database.azure.com:5432/studentdb?sslmode=require"
$env:DB_USERNAME = "<admin-username>"
$env:DB_PASSWORD = "<admin-password>"

.\gradlew.bat bootRun
```

**Example with actual values:**

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DB_URL      = "jdbc:postgresql://java-practice-springboot.postgres.database.azure.com:5432/studentdb?sslmode=require"
$env:DB_USERNAME = "springdb"
$env:DB_PASSWORD = "Test_123"

.\gradlew.bat bootRun
```

### Option 2: Use the helper script

```powershell
.\scripts\run-azure-postgres.ps1 `
    -ServerName "java-practice-springboot" `
    -DatabaseName "studentdb" `
    -DbUsername "springdb" `
    -DbPassword "Test_123"
```

> If auth fails, add `-UseServerQualifiedUsername` to send `springdb@java-practice-springboot`.

### Azure PostgreSQL Connection Details

| Setting | Value |
|---|---|
| Server | `java-practice-springboot.postgres.database.azure.com` |
| Port | `5432` |
| Database | `studentdb` |
| Username | `springdb` |
| Password | `Test_123` |
| SSL | `require` |

---

## 🔍 Azure Cloud Shell — psql Commands

### One-liner queries (from bash — no interactive session needed)

```bash
# List all tables
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "\dt"

# View all students
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "SELECT * FROM students;"

# View all teachers
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "SELECT * FROM teachers;"

# Describe table structure
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "\d students"
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "\d teachers"
```

### Interactive session

```bash
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require"
```

Once connected (`studentdb=>`):

```sql
\dt                      -- list all tables
SELECT * FROM students;  -- view students
SELECT * FROM teachers;  -- view teachers
\d students              -- describe students table
\d teachers              -- describe teachers table
\q                       -- exit psql
```

---

## 🔥 Azure Firewall Setup

1. **Azure Portal** → your PostgreSQL server → **Networking**
2. ✅ Check **Allow public access through the internet using a public IP address**
3. ✅ Check **Allow public access from any Azure service within Azure to this server**
4. Click **+ Add current client IP address** to whitelist your IP
5. Click **Save**

> **Important:** Azure Cloud Shell connects from an Azure IP, so the "Allow Azure services" checkbox must be enabled.

---

## 📡 API Endpoints

### Students — `/api/students`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/api/students` | Create a student | `201 Created` |
| `GET` | `/api/students` | List all students | `200 OK` |
| `GET` | `/api/students/{id}` | Get student by ID | `200 OK` |
| `PUT` | `/api/students/{id}` | Update a student | `200 OK` |
| `DELETE` | `/api/students/{id}` | Delete a student | `204 No Content` |

**Sample request body (POST / PUT):**

```json
{
  "firstName": "Andrew",
  "lastName": "Candare",
  "email": "andrew@example.com",
  "age": 22,
  "course": "Computer Science"
}
```

### Teachers — `/api/teachers`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/api/teachers` | Create a teacher | `201 Created` |
| `GET` | `/api/teachers` | List all teachers | `200 OK` |
| `GET` | `/api/teachers/{id}` | Get teacher by ID | `200 OK` |
| `DELETE` | `/api/teachers/{id}` | Delete a teacher | `204 No Content` |

**Sample request body (POST):**

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "course": "Mathematics"
}
```

### Swagger UI

Visit **`http://localhost:7000/swagger-ui/index.html`** to test all endpoints interactively.

---

## 🗄️ Database Schema

Tables are auto-created by Hibernate (`ddl-auto: update`).

### `students`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `first_name` | `VARCHAR(100)` | NOT NULL |
| `last_name` | `VARCHAR(100)` | NOT NULL |
| `email` | `VARCHAR(150)` | NOT NULL, UNIQUE |
| `age` | `INTEGER` | NOT NULL |
| `course` | `VARCHAR(100)` | NOT NULL |

### `teachers`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `first_name` | `VARCHAR(100)` | NOT NULL |
| `last_name` | `VARCHAR(100)` | NOT NULL |
| `course` | `VARCHAR(100)` | NOT NULL |

---

## 🔄 Request Flow

```
Client (HTTP Request)
    │
    ▼
Controller          — validates input, maps DTO
    │
    ▼
Service (Interface) — business contract
    │
    ▼
Implementation      — business logic, calls repository
    │
    ▼
Repository          — Spring Data JPA → database query
    │
    ▼
Entity              — JPA maps to DB table row
    │
    ▼
Database (H2 or Azure PostgreSQL)
```
1. Client sends HTTP request to Controller
2. Controller validates `@RequestBody` and calls Service interface
3. Implementation applies business logic, calls Repository
4. Repository executes DB operation on Entity
5. Implementation maps Entity → ResponseDto
6. Controller returns JSON response to client

---

## ⚙️ Configuration Files

| File | Purpose |
|---|---|
| `application.yml` | App name, port (`7000`), profile selector (default: `h2`) |
| `application-h2.yml` | H2 in-memory DB, H2 console enabled at `/h2-console` |
| `application-postgres.yml` | Azure PostgreSQL via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` env vars |
| `build.gradle` | Dependencies: Spring Boot, JPA, H2, PostgreSQL, Lombok, SpringDoc |
| `gradle.properties` | JDK home path |
| `scripts/run-azure-postgres.ps1` | PowerShell helper to set env vars + launch app |

### Switching profiles

```powershell
# Use H2 (default — no env var needed)
.\gradlew.bat bootRun

# Use Azure PostgreSQL
$env:SPRING_PROFILES_ACTIVE = "postgres"
.\gradlew.bat bootRun
```

---

## 📦 Git Commands Guide

### First-time setup

```bash
# Configure your identity (one-time)
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

### Initialize & push to GitHub (new repo)

```bash
# Initialize git in project root
git init

# Add all files
git add .

# First commit
git commit -m "Initial commit: Spring Boot Student & Teacher CRUD with Azure PostgreSQL"

# Add remote (replace with your repo URL)
git remote add origin https://github.com/<your-username>/SampleSpringBootApplication.git

# Push to main branch
git push -u origin main
```

### Daily workflow

```bash
# Check what changed
git status

# See detailed changes
git diff

# Stage all changes
git add .

# Stage specific file
git add src/main/java/com/codeWithJeff/SampleSpringBootApplication/Controller/StudentController.java

# Commit with message
git commit -m "Add teacher CRUD endpoints"

# Push to remote
git push
```

### Branching

```bash
# Create and switch to new branch
git checkout -b feature/add-pagination

# List all branches
git branch -a

# Switch to existing branch
git checkout main

# Merge feature branch into main
git checkout main
git merge feature/add-pagination

# Delete branch after merge
git branch -d feature/add-pagination
```

### Pulling & syncing

```bash
# Pull latest changes from remote
git pull

# Pull with rebase (cleaner history)
git pull --rebase origin main
```

### Undoing things

```bash
# Unstage a file (keep changes)
git reset HEAD <file>

# Discard all uncommitted changes
git checkout -- .

# Undo last commit (keep changes staged)
git reset --soft HEAD~1

# Undo last commit (discard changes completely)
git reset --hard HEAD~1
```

### Viewing history

```bash
# View commit log
git log --oneline

# View log with graph
git log --oneline --graph --all

# See who changed what in a file
git blame src/main/java/com/codeWithJeff/SampleSpringBootApplication/Entity/Student.java
```

### Tagging releases

```bash
# Create a tag
git tag -a v1.0.0 -m "First stable release with Student & Teacher CRUD"

# Push tags to remote
git push origin --tags
```

### .gitignore essentials

Make sure your `.gitignore` includes:

```
# Gradle
.gradle/
build/

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Environment (NEVER commit secrets)
.env
```

---

## 🔧 Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `password authentication failed` | Wrong username or password | Verify credentials; use `springdb` (not `springdb@server`) for Flexible Server |
| `connection refused` | IP not whitelisted | Azure Portal → Networking → Add your client IP → Save |
| `database "studentdb" does not exist` | DB not created | Azure Portal → Databases → Add `studentdb` |
| `bash: SELECT: command not found` | Typed SQL in bash, not psql | Use `PGPASSWORD=... psql ... -c "SELECT ..."` or open interactive psql first |
| `relation "students" does not exist` | Tables not created | Run Spring Boot app once with `ddl-auto: update` to auto-create tables |
| App uses H2 instead of PostgreSQL | Profile not set | Set `$env:SPRING_PROFILES_ACTIVE = "postgres"` before `bootRun` |
| `Port 7000 already in use` | Another instance running | Kill it or change port in `application.yml` |

---

## 💡 Practice Ideas

- [ ] Add `PUT /api/teachers/{id}` update endpoint
- [ ] Add pagination and sorting on GET endpoints
- [ ] Add search endpoints (`findByEmail`, `findByCourse`)
- [ ] Add global exception handling with `@ControllerAdvice`
- [ ] Add unit tests for service layer with Mockito
- [ ] Add `createdAt` / `updatedAt` timestamps to entities
- [ ] Add Student ↔ Teacher relationship (Many-to-One)
- [ ] Dockerize the application
- [ ] Deploy to Azure App Service
- [ ] Move secrets to Azure Key Vault

---

## 📚 Documentation

| Document | Description |
|---|---|
| [`docs/AZURE_POSTGRES_SETUP.md`](docs/AZURE_POSTGRES_SETUP.md) | Azure PostgreSQL integration guide |
| [`docs/COMPLETE_PROJECT_GUIDE.md`](docs/COMPLETE_PROJECT_GUIDE.md) | Full project walkthrough |
| [`docs/REQUEST_FLOW_GUIDE.md`](docs/REQUEST_FLOW_GUIDE.md) | Step-by-step request flow explanation |
| [`docs/DOCUMENTATION_INDEX.md`](docs/DOCUMENTATION_INDEX.md) | Index of all documentation |

---

## 📄 License

This project is for learning and practice purposes.

