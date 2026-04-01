# Azure PostgreSQL Integration Guide

**Last Updated:** April 1, 2026

This guide connects the Spring Boot project to Azure Database for PostgreSQL Flexible Server.

## 1) Prerequisites

- Azure PostgreSQL Flexible Server is deployed
- Database exists (example: `studentdb`)
- Your client IP is added in server networking firewall rules
- `application-postgres.yml` is present and uses `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

## 2) Connection values you need

From Azure Portal -> your PostgreSQL server:

- Server host: `<server-name>.postgres.database.azure.com`
- Port: `5432`
- Database: `studentdb`
- Admin login: `<admin-username>`
- Admin password: `<admin-password>`

## 3) Run app against Azure PostgreSQL (PowerShell)

From project root:

```powershell
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://<server-name>.postgres.database.azure.com:5432/studentdb?sslmode=require"
$env:DB_USERNAME="<admin-username>"
$env:DB_PASSWORD="<admin-password>"
.\gradlew.bat bootRun
```

If authentication fails once, retry username as:

```powershell
$env:DB_USERNAME="<admin-username>@<server-name>"
.\gradlew.bat bootRun
```

## 4) Test connectivity before running (PowerShell)

```powershell
# Test TCP connection to Azure PostgreSQL
Test-NetConnection -ComputerName java-practice-springboot.postgres.database.azure.com -Port 5432

# Expected: TcpTestSucceeded = True
# If TcpTestSucceeded = False: check firewall rules or org network blocking port 5432
```

```powershell
# Test connectivity to port 443 (to verify general Azure access)
Test-NetConnection -ComputerName java-practice-springboot.postgres.database.azure.com -Port 443
```

## 5) Verify from API

In a second terminal:

```powershell
Invoke-RestMethod -Uri "http://localhost:7000/api/students"
Invoke-RestMethod -Uri "http://localhost:7000/api/teachers"
```

Then test create endpoint and re-check list endpoint.

## 6) Azure Cloud Shell — psql Commands

### One-liner queries (from Azure Cloud Shell bash)

```bash
# List all tables
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "\dt"

# Describe table structure
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "\d students"
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "\d teachers"
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "\d subject"

# Query all data
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "SELECT * FROM students;"
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "SELECT * FROM teachers;"
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "SELECT * FROM subject;"

# Filtered queries
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "SELECT * FROM teachers WHERE teacher_id = 1;"
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "SELECT COUNT(*) FROM subject;"
```

### Interactive session

```bash
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require"
```

Once connected (`studentdb=>`):

```sql
\dt                      -- list all tables
\d students              -- describe students table
\d teachers              -- describe teachers table
\d subject               -- describe subject table
SELECT * FROM students;  -- view students
SELECT * FROM teachers;  -- view teachers
SELECT * FROM subject;   -- view subjects with FK references
\q                       -- exit psql
```

### Drop and recreate tables (schema reset)

Use this if entity changes cause schema mismatch errors (e.g., old `course` column still in `teachers` table):

```bash
PGPASSWORD='Test_123' psql "host=java-practice-springboot.postgres.database.azure.com port=5432 dbname=studentdb user=springdb sslmode=require" -c "DROP TABLE IF EXISTS subject, teachers, students CASCADE;"
```

Then restart the app — Hibernate `ddl-auto: update` will recreate all tables with the current entity structure.

### Azure CLI — List databases

```bash
az postgres flexible-server db list --server-name java-practice-springboot --resource-group Java-Practice-Spring-Boot
```

## 7) Schema Management — Important Notes

- **`ddl-auto: update`** only ADDS columns/tables, it never DROPS columns
- If you remove a field from an entity (e.g., removed `course` from `Teacher`), the old column stays in the DB
- Old `NOT NULL` columns will cause 500 errors on INSERT because no value is provided
- **Fix:** Drop and recreate tables (see command above) or use `ddl-auto: create-drop` temporarily

### Current Database Schema (April 1, 2026)

```sql
-- students table
CREATE TABLE students (
    student_id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    age INTEGER NOT NULL,
    course VARCHAR(100) NOT NULL
);

-- teachers table (NO course column — removed April 1)
CREATE TABLE teachers (
    teacher_id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL
);

-- subject table (NEW — April 1)
CREATE TABLE subject (
    subject_id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    teacher_id BIGINT NOT NULL UNIQUE REFERENCES teachers(teacher_id),
    subject VARCHAR(100) NOT NULL
);
```

## 8) Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `Connection refused` | IP not whitelisted | Azure Portal → Networking → Add your client IP → Save |
| `TcpTestSucceeded: False` | Org firewall blocks port 5432 | Try mobile hotspot, or check if org allows outbound 5432 |
| `password authentication failed` | Wrong username or password | Verify credentials; try `springdb` or `springdb@java-practice-springboot` |
| `database does not exist` | DB not created | Azure Portal → Databases → Add `studentdb` |
| `null value in column "course"` | Old `course NOT NULL` column still in DB | Drop tables and restart (see schema reset command above) |
| `bash: SELECT: command not found` | Typed SQL in bash, not psql | Use `PGPASSWORD=... psql ... -c "SELECT ..."` or open interactive psql first |
| `relation "subject" does not exist` | Tables not created | Run app once with `ddl-auto: update` to auto-create tables |

## 9) Security recommendations

- Do not hardcode DB credentials in Git-tracked files
- Rotate password after sharing it accidentally
- Prefer a dedicated app user instead of using admin account
- Later, move secrets to Azure Key Vault
