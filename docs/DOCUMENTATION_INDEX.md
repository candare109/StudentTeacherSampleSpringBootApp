# Documentation Index

## Available Documentation

- `docs/COMPLETE_PROJECT_GUIDE.md`
  - Full project structure, layered architecture, DTO/entity/repository/service/controller relationships, and endpoint reference.

- `docs/REQUEST_FLOW_GUIDE.md`
  - Step-by-step request flow using `GET /api/students/{id}`.

- `docs/AZURE_POSTGRES_SETUP.md`
  - Azure Database for PostgreSQL Flexible Server integration steps, environment variables, and troubleshooting.

- `README.md`
  - Quick start, endpoints, profiles, and Azure run examples.

## Suggested Reading Order

1. `README.md`
2. `docs/COMPLETE_PROJECT_GUIDE.md`
3. `docs/REQUEST_FLOW_GUIDE.md`
4. `docs/AZURE_POSTGRES_SETUP.md`

## Current Profile Setup

- Default active profile: `postgres`
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

