# 🔐 Azure Key Vault Setup — Step-by-Step Guide

Move your database credentials and JWT secret from environment variables to **Azure Key Vault**,
so your Spring Boot app fetches secrets securely at startup.

**Last Updated:** April 7, 2026

---

## 📑 Table of Contents

- [Prerequisites](#prerequisites)
- [Part 1 — Create the Key Vault in Azure Portal](#part-1--create-the-key-vault-in-azure-portal)
  - [Tab 1: Basics](#tab-1-basics)
  - [Tab 2: Access Configuration](#tab-2-access-configuration)
  - [Tab 3: Networking](#tab-3-networking)
  - [Tab 4: Tags](#tab-4-tags)
  - [Tab 5: Review + Create](#tab-5-review--create)
- [Part 2 — Add Secrets to the Key Vault](#part-2--add-secrets-to-the-key-vault)
- [Part 3 — Grant Your Account Access (for local dev)](#part-3--grant-your-account-access-for-local-dev)
- [Part 4 — Spring Boot Code Changes](#part-4--spring-boot-code-changes)
- [Part 5 — Run and Test Locally](#part-5--run-and-test-locally)
- [Part 6 — How It Works (Explanation)](#part-6--how-it-works-explanation)
- [Part 7 — Verify Secrets Are Being Read](#part-7--verify-secrets-are-being-read)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

- Azure account with an active subscription
- Azure CLI installed (`az --version` to check)
- Logged in: `az login`
- Your existing Azure PostgreSQL server is running
  - Server: `java-practice-springboot.postgres.database.azure.com`
  - Database: `studentdb`
  - Resource Group: `Java-Practice-Spring-Boot`

---

## Part 1 — Create the Key Vault in Azure Portal

Go to: **Azure Portal** → Search **"Key vaults"** → Click **"+ Create"**

---

### Tab 1: Basics

| Field | Value | Why |
|-------|-------|-----|
| **Subscription** | *(your subscription)* | Where billing goes |
| **Resource group** | `Java-Practice-Spring-Boot` | Same resource group as your PostgreSQL server — keeps everything together |
| **Key vault name** | `springboot-practice-kv` | Must be globally unique across all of Azure. This becomes your vault URL: `https://springboot-practice-kv.vault.azure.net/` |
| **Region** | Same as your PostgreSQL server (e.g., `East US`) | Lower latency — app and vault in the same region |
| **Pricing tier** | **Standard** | Standard is fine for practice. Premium adds HSM-backed keys (not needed) |
| **Days to retain deleted vaults** | `7` (minimum) | Soft-delete protection. For practice, 7 days is fine |
| **Purge protection** | **Disabled** | Leave disabled for practice — lets you fully delete the vault if needed. In production, enable this |

Click **"Next: Access configuration >"**

---

### Tab 2: Access Configuration

This is the most important tab — it controls WHO can read your secrets.

| Field | Value | Why |
|-------|-------|-----|
| **Permission model** | **Azure role-based access control (RBAC)** | Recommended by Microsoft. Uses Azure IAM roles instead of legacy vault policies. Easier to manage |

> **RBAC vs Vault Access Policy — What's the difference?**
>
> | Model | How permissions work | Best for |
> |-------|---------------------|----------|
> | **Azure RBAC** (recommended) | Uses Azure IAM roles (`Key Vault Secrets User`, etc.) assigned in Access Control (IAM) | New projects, follows Azure best practices |
> | **Vault access policy** (legacy) | Uses per-vault policies configured inside Key Vault settings | Older setups, some tutorials still use this |
>
> Pick **RBAC**. If a tutorial says "add an access policy," just assign an IAM role instead.

**Resource access** (checkboxes at bottom):

| Checkbox | Value | Why |
|----------|-------|-----|
| Azure Virtual Machines for deployment | ☐ Unchecked | Not using VMs |
| Azure Resource Manager for template deployment | ☐ Unchecked | Not using ARM templates |
| Azure Disk Encryption for volume encryption | ☐ Unchecked | Not encrypting disks |

Click **"Next: Networking >"**

---

### Tab 3: Networking

| Field | Value | Why |
|-------|-------|-----|
| **Enable public access** | ✅ **Checked** (All networks) | For practice/local dev, your machine needs to reach Key Vault over the internet. In production, you'd restrict this |
| **Network connectivity** | **Allow public access from all networks** | Simplest for development. Your `az login` and Spring Boot app will connect over HTTPS (port 443) |

> **Production note:** In a real production setup, you would:
> - Select "Allow public access from specific virtual networks and IP addresses"
> - Or select "Disable public access" and use a Private Endpoint
> - For practice, "All networks" is fine

**Private endpoint** section:
- Skip — don't add any. Not needed for practice.

Click **"Next: Tags >"**

---

### Tab 4: Tags

Tags are optional labels for organizing Azure resources. Add these for good practice:

| Name | Value |
|------|-------|
| `project` | `SampleSpringBootApplication` |
| `environment` | `practice` |
| `owner` | `jeff` |

> Tags don't affect functionality. They help you identify and filter resources later
> (e.g., "show me all resources tagged `project: SampleSpringBootApplication`").

Click **"Next: Review + create >"**

---

### Tab 5: Review + Create

You'll see a summary like this:

```
Subscription:           (your subscription)
Resource group:         Java-Practice-Spring-Boot
Key vault name:         springboot-practice-kv
Region:                 East US
Pricing tier:           Standard
Soft-delete:            Enabled (7 days retention)
Purge protection:       Disabled
Permission model:       Azure role-based access control
Public network access:  All networks
```

✅ Click **"Create"**

Wait ~30 seconds for deployment. Once complete, click **"Go to resource"**.

Your Key Vault URL is: `https://springboot-practice-kv.vault.azure.net/`

---

## Part 2 — Add Secrets to the Key Vault

### Via Azure Portal (GUI)

1. Open your Key Vault → Click **"Secrets"** in the left sidebar → Click **"+ Generate/Import"**

2. Add each secret one by one:

| Secret Name | Value | Notes |
|-------------|-------|-------|
| `DB-URL` | `jdbc:postgresql://java-practice-springboot.postgres.database.azure.com:5432/studentdb?sslmode=require` | Your full JDBC URL |
| `DB-USERNAME` | `springdb` | Your PostgreSQL username |
| `DB-PASSWORD` | *(your actual password)* | Your PostgreSQL password |
| `JWT-SECRET` | *(a long secret string, at least 32 characters)* | For JWT signing (if you've added JWT auth) |

For each secret:
- **Upload options:** Manual
- **Name:** e.g., `DB-URL`
- **Secret value:** e.g., `jdbc:postgresql://...`
- **Content type:** (leave blank)
- **Set activation date:** ☐ unchecked
- **Set expiration date:** ☐ unchecked
- **Enabled:** ✅ Yes
- Click **"Create"**

> ⚠️ **Key Vault secret names do NOT allow underscores.** Use hyphens instead:
> - ❌ `DB_URL` → Azure rejects this
> - ✅ `DB-URL` → Spring Cloud Azure maps this to the Spring property `DB-URL`
>
> Your YAML placeholders will use `${DB-URL}` (hyphens) to match the Key Vault secret names exactly.

### Via Azure CLI (Alternative)

```powershell
az keyvault secret set --vault-name springboot-practice-kv --name "DB-URL" --value "jdbc:postgresql://java-practice-springboot.postgres.database.azure.com:5432/studentdb?sslmode=require"

az keyvault secret set --vault-name springboot-practice-kv --name "DB-USERNAME" --value "springdb"

az keyvault secret set --vault-name springboot-practice-kv --name "DB-PASSWORD" --value "YourPasswordHere"

az keyvault secret set --vault-name springboot-practice-kv --name "JWT-SECRET" --value "my-super-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm-ok"
```

### Verify secrets were created:

```powershell
az keyvault secret list --vault-name springboot-practice-kv --output table
```

---

## Part 3 — Grant Your Account Access (for local dev)

Since you picked **RBAC** as the permission model, you need to assign yourself the
**"Key Vault Secrets User"** role so your app (via `az login`) can read secrets.

### Via Azure Portal (GUI)

1. Open your Key Vault → Click **"Access control (IAM)"** in the left sidebar
2. Click **"+ Add"** → **"Add role assignment"**
3. **Role tab:** Search and select **`Key Vault Secrets User`** → Click Next
4. **Members tab:**
   - **Assign access to:** User, group, or service principal
   - Click **"+ Select members"**
   - Search for your email/name → select yourself → Click "Select"
5. Click **"Review + assign"** → Click **"Review + assign"** again

### Via Azure CLI (Alternative)

```powershell
# Get your Azure AD user object ID
$userId = az ad signed-in-user show --query id --output tsv

# Get the Key Vault resource ID
$kvId = az keyvault show --name springboot-practice-kv --query id --output tsv

# Assign "Key Vault Secrets User" role
az role assignment create --role "Key Vault Secrets User" --assignee $userId --scope $kvId
```

> **Wait 1-2 minutes** after assigning the role for it to take effect (Azure RBAC propagation).

### Verify access:

```powershell
az keyvault secret show --vault-name springboot-practice-kv --name "DB-URL" --query value --output tsv
```

If this returns your JDBC URL, access is working.

---

## Part 4 — Spring Boot Code Changes

### 4a. Add dependency to `build.gradle`

Add inside `dependencies { }`:

```groovy
// Spring Cloud Azure — loads Key Vault secrets as Spring properties
implementation 'com.azure.spring:spring-cloud-azure-starter-keyvault-secrets:5.21.0'
```

Full dependencies block becomes:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'com.azure.spring:spring-cloud-azure-starter-keyvault-secrets:5.21.0'   // ← NEW
    compileOnly 'org.projectlombok:lombok'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.postgresql:postgresql'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

Then refresh dependencies:

```powershell
.\gradlew build --refresh-dependencies
```

### 4b. Update `application-postgres.yml`

Add the `spring.cloud.azure.keyvault.secret` block and change placeholders to use hyphens:

```yaml
spring:
  datasource:
    url: ${DB-URL}
    username: ${DB-USERNAME}
    password: ${DB-PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  cloud:
    azure:
      keyvault:
        secret:
          endpoint: https://springboot-practice-kv.vault.azure.net/
```

> **Important change:** The placeholders change from `${DB_URL}` (underscores) to `${DB-URL}` (hyphens)
> to match the Key Vault secret names exactly. Spring resolves `${DB-URL}` from the Key Vault secret `DB-URL`.

### 4c. Update `application.yml` (for JWT secret, if using JWT)

```yaml
spring:
  application:
    name: SampleSpringBootApplication
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:h2}

server:
  port: 7000

jwt:
  secret: ${JWT-SECRET:default-dev-secret-that-is-at-least-256-bits-long-for-hs256-algorithm}
  expiration: 86400000
```

> The `${JWT-SECRET:default-dev-secret...}` syntax means:
> - When running with `postgres` profile → reads `JWT-SECRET` from Key Vault ✅
> - When running with `h2` profile → Key Vault isn't configured, uses the fallback value after `:` ✅

### 4d. `application-h2.yml` — NO CHANGES needed

H2 profile doesn't use Key Vault. No changes required. It keeps working as-is.

### 4e. `SampleSpringBootApplication.java` — NO CHANGES needed

Your main class stays the same:

```java
@SpringBootApplication    // already includes @EnableAutoConfiguration
public class SampleSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleSpringBootApplication.class, args);
    }
}
```

> Note: `@SpringBootApplication` already includes `@EnableAutoConfiguration`.
> You do NOT need to add `@EnableAutoConfiguration` separately.

---

## Part 5 — Run and Test Locally

### Step 1: Make sure you're logged into Azure CLI

```powershell
az login
```

### Step 2: Run with postgres profile

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
.\gradlew bootRun
```

That's it. **No more `$env:DB_URL`, `$env:DB_USERNAME`, `$env:DB_PASSWORD`.**
Spring Cloud Azure fetches them from Key Vault automatically.

### Step 3: Test an endpoint

```powershell
Invoke-RestMethod -Uri "http://localhost:7000/api/students"
```

### Compare: Before vs After

**Before (environment variables):**

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DB_URL = "jdbc:postgresql://java-practice-springboot.postgres.database.azure.com:5432/studentdb?sslmode=require"
$env:DB_USERNAME = "springdb"
$env:DB_PASSWORD = "YourPasswordHere"
.\gradlew bootRun
```

**After (Key Vault):**

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
.\gradlew bootRun
```

> Credentials are no longer in your terminal history, PowerShell scripts, or environment.

---

## Part 6 — How It Works (Explanation)

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          APP STARTUP FLOW                                │
└──────────────────────────────────────────────────────────────────────────┘

1. You run: .\gradlew bootRun (with profile "postgres")
       │
       ▼
2. Spring loads: application.yml + application-postgres.yml
       │
       ▼
3. Spring sees: spring.cloud.azure.keyvault.secret.endpoint is set
       │
       ▼
4. Spring Cloud Azure activates:
       │
       ├── Uses DefaultAzureCredential to authenticate:
       │     Tries in order:
       │       a) Environment vars (AZURE_CLIENT_ID, etc.) — not set, skip
       │       b) Managed Identity — not on Azure, skip
       │       c) Azure CLI ("az login") — ✅ FOUND, uses this
       │
       ▼
5. Connects to: https://springboot-practice-kv.vault.azure.net/
       │
       ▼
6. Fetches ALL secrets from Key Vault:
       │     DB-URL = "jdbc:postgresql://java-practice-springboot..."
       │     DB-USERNAME = "springdb"
       │     DB-PASSWORD = "YourPasswordHere"
       │     JWT-SECRET = "my-super-secret-key..."
       │
       ▼
7. Registers them as Spring property sources:
       │     ${DB-URL} → "jdbc:postgresql://..."
       │     ${DB-USERNAME} → "springdb"
       │     ${DB-PASSWORD} → "YourPasswordHere"
       │     ${JWT-SECRET} → "my-super-secret-key..."
       │
       ▼
8. Spring resolves placeholders in application-postgres.yml:
       │     spring.datasource.url = "jdbc:postgresql://..."
       │     spring.datasource.username = "springdb"
       │     spring.datasource.password = "YourPasswordHere"
       │
       ▼
9. DataSource bean is created → connects to PostgreSQL ✅
   JwtService reads jwt.secret → uses signing key ✅
       │
       ▼
10. App is running. No secrets in code, config, or terminal.
```

### DefaultAzureCredential — How authentication works

The Spring Cloud Azure library uses `DefaultAzureCredential` which tries multiple auth methods in order:

| # | Method | When it's used |
|---|--------|---------------|
| 1 | Environment variables (`AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID`) | CI/CD pipelines |
| 2 | Managed Identity | App deployed to Azure App Service / Azure VM |
| 3 | Azure CLI (`az login`) | **Local development** ← this is what you use |
| 4 | IntelliJ / VS Code Azure plugin credentials | If IDE Azure plugin is installed |

You don't need to configure which one to use — it automatically picks the first one that works.

---

## Part 7 — Verify Secrets Are Being Read

### Check from Azure CLI that secrets exist:

```powershell
# List all secrets (names only)
az keyvault secret list --vault-name springboot-practice-kv --query "[].name" --output tsv

# Read a specific secret value
az keyvault secret show --vault-name springboot-practice-kv --name "DB-URL" --query value --output tsv
```

### Add debug logging to see Key Vault being loaded (temporary):

Add to `application-postgres.yml` temporarily:

```yaml
logging:
  level:
    com.azure.spring: DEBUG
    com.azure.security.keyvault: DEBUG
```

You should see log lines like:

```
Fetching secrets from Key Vault: https://springboot-practice-kv.vault.azure.net/
Retrieved secret: DB-URL
Retrieved secret: DB-USERNAME
Retrieved secret: DB-PASSWORD
```

Remove the debug logging after verifying.

---

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `SecretClient is not configured` | Missing `endpoint` in YAML | Add `spring.cloud.azure.keyvault.secret.endpoint` to `application-postgres.yml` |
| `Status code 401 Unauthorized` from Key Vault | No access to Key Vault | Assign "Key Vault Secrets User" role (see Part 3) |
| `Status code 403 Forbidden` from Key Vault | Role not propagated yet, or wrong vault name | Wait 2 minutes; verify vault name is correct |
| `DefaultAzureCredential failed` | Not logged into Azure CLI | Run `az login` |
| `Could not resolve placeholder 'DB-URL'` | Secret doesn't exist in Key Vault | Check secret name matches exactly: `DB-URL` (not `DB_URL`) |
| App works with H2 but fails with postgres | Key Vault config only loads with postgres profile | That's correct — H2 doesn't need Key Vault |
| `EnvironmentCredential authentication failed` | Tried env var auth but vars not set | That's fine — it falls through to Azure CLI auth. Ignore this log line |
| Build fails after adding dependency | Dependency version issue | Run `.\gradlew build --refresh-dependencies` |
| `Connection refused` to PostgreSQL | Key Vault works but DB firewall blocks your IP | Azure Portal → PostgreSQL → Networking → add your client IP |

---

## Summary of All Changes

| File | Change |
|------|--------|
| `build.gradle` | Add `com.azure.spring:spring-cloud-azure-starter-keyvault-secrets:5.21.0` |
| `application-postgres.yml` | Add `spring.cloud.azure.keyvault.secret.endpoint`, change `${DB_URL}` → `${DB-URL}` (same for USERNAME, PASSWORD) |
| `application.yml` | Change `${JWT_SECRET}` → `${JWT-SECRET:fallback}` (only if using JWT) |
| `application-h2.yml` | No changes |
| `SampleSpringBootApplication.java` | No changes |
| All controllers / services | No changes |

**Total code changes: 1 dependency + ~4 lines of YAML. Everything else stays the same.**

---

## What's Safe to Push to GitHub

| File | Has real secrets? | Safe to push? |
|------|------------------|--------------|
| `application.yml` | No | ✅ Yes |
| `application-h2.yml` | No (default H2 dev creds) | ✅ Yes |
| `application-postgres.yml` | No (only `${...}` placeholders + vault URL) | ✅ Yes |
| `build.gradle` | No | ✅ Yes |
| `.env` file (if you create one) | **YES — real secrets** | ❌ **NEVER push** — add to `.gitignore` |

The Key Vault URL (`https://springboot-practice-kv.vault.azure.net/`) is safe to push — knowing the URL alone doesn't grant access. Only authenticated users with the right IAM role can read secrets.

