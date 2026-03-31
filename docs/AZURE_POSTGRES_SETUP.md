# Azure PostgreSQL Integration Guide

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

## 4) Verify from API

In a second terminal:

```powershell
Invoke-RestMethod -Uri "http://localhost:7000/api/students"
```

Then test create endpoint and re-check list endpoint.

## 5) Troubleshooting

- `Connection refused`:
  - Check server firewall allows your current public IP
  - Check org network allows outbound `5432`
- `password authentication failed`:
  - Recheck username format
  - Recheck password value
- `database does not exist`:
  - Create `studentdb` in Azure

## 6) Security recommendations

- Do not hardcode DB credentials in Git-tracked files
- Rotate password after sharing it accidentally
- Prefer a dedicated app user instead of using admin account
- Later, move secrets to Azure Key Vault

