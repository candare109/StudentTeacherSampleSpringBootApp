# 🌐 Eureka Service Discovery Guide — Server & Client

**Last Updated:** April 8, 2026

A complete guide to setting up **Netflix Eureka** for service discovery with Spring Boot.
Covers: creating a Eureka server, registering this project as a client, inter-service communication, and production considerations.

---

## 📑 Table of Contents

- [What is Service Discovery?](#-what-is-service-discovery)
- [Eureka Architecture](#-eureka-architecture)
- [Part 1 — Create the Eureka Server](#part-1--create-the-eureka-server)
- [Part 2 — Register This Project as a Eureka Client](#part-2--register-this-project-as-a-eureka-client)
- [Part 3 — Inter-Service Communication](#part-3--inter-service-communication)
- [Part 4 — Multiple Microservices Example](#part-4--multiple-microservices-example)
- [Part 5 — API Gateway with Eureka](#part-5--api-gateway-with-eureka)
- [Health Checks and Monitoring](#-health-checks-and-monitoring)
- [Production Considerations](#-production-considerations)
- [Troubleshooting Eureka](#-troubleshooting-eureka)
- [Quick Reference — Annotations & Properties](#-quick-reference--annotations--properties)

---

## 📖 What is Service Discovery?

In a **monolith**, everything is in one app. In **microservices**, features are split into separate apps that need to find each other.

**Without service discovery:**
```
Order Service → needs to call → User Service at http://192.168.1.50:8081
                                 ↑ What if this IP changes?
                                 ↑ What if there are 3 instances?
                                 ↑ What if one instance goes down?
```

**With Eureka service discovery:**
```
Order Service → asks Eureka → "Where is user-service?"
Eureka replies → "It's at 192.168.1.50:8081 and 192.168.1.51:8081"
Order Service → picks one and makes the call
```

**Key benefits:**
1. **No hardcoded URLs** — services find each other by name
2. **Load balancing** — multiple instances of the same service
3. **Health monitoring** — Eureka removes dead instances automatically
4. **Dynamic scaling** — new instances register automatically

---

## 🏗️ Eureka Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    EUREKA SERVICE DISCOVERY                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    ┌───────────────────┐                         │
│                    │   EUREKA SERVER    │                         │
│                    │   (Registry)       │                         │
│                    │                   │                          │
│                    │ Service Registry: │                          │
│                    │ ┌───────────────┐ │                          │
│                    │ │student-service│ │                          │
│                    │ │ → :7000       │ │                          │
│                    │ │ → :7001       │ │                          │
│                    │ ├───────────────┤ │                          │
│                    │ │order-service  │ │                          │
│                    │ │ → :8080       │ │                          │
│                    │ ├───────────────┤ │                          │
│                    │ │payment-service│ │                          │
│                    │ │ → :9090       │ │                          │
│                    │ └───────────────┘ │                          │
│                    └──────┬──────┬─────┘                         │
│                    ▲      │      │      ▲                        │
│          register  │      │      │      │  register              │
│          + heartbeat      │      │      + heartbeat              │
│                    │      │      │      │                         │
│        ┌───────────┘      │      └──────┴──────────┐             │
│        │                  │                         │             │
│  ┌─────┴──────┐    ┌─────┴──────┐    ┌─────────────┴─┐          │
│  │ student-   │    │ order-     │    │ payment-      │           │
│  │ service    │    │ service    │    │ service       │           │
│  │ :7000      │◄──►│ :8080      │◄──►│ :9090         │          │
│  └────────────┘    └────────────┘    └───────────────┘           │
│                                                                  │
│  Services call each other by NAME (not IP):                      │
│  restClient.get("http://student-service/api/students/1")         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**How it works:**
1. **Eureka Server** starts first — it's the central registry
2. **Each service** (client) registers itself with Eureka on startup
3. **Every 30 seconds**, each client sends a heartbeat to Eureka ("I'm still alive")
4. **When a service needs another**, it asks Eureka for the address by service name
5. **If a service dies**, Eureka removes it after missing 3 heartbeats (90 seconds)

---

## Part 1 — Create the Eureka Server

The Eureka Server is a **separate Spring Boot project**. It runs independently from your application services.

### 1.1 — Create a New Project

Use [Spring Initializr](https://start.spring.io/) or create manually:

**Project structure:**
```
eureka-server/
├── build.gradle
├── settings.gradle
├── src/main/java/com/codeWithJeff/eurekaserver/
│   └── EurekaServerApplication.java
└── src/main/resources/
    └── application.yml
```

### 1.2 — build.gradle

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.5'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.codeWithJeff'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// Spring Cloud BOM — manages all Spring Cloud dependency versions
// IMPORTANT: Use the version compatible with your Spring Boot version
dependencyManagement {
    imports {
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:2024.0.1'
    }
}

dependencies {
    // This single dependency includes the Eureka Server
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

> **Version compatibility:**
> | Spring Boot | Spring Cloud |
> |---|---|
> | 3.4.x | 2024.0.x |
> | 3.3.x | 2023.0.x |
> | 3.2.x | 2023.0.x |

### 1.3 — settings.gradle

```groovy
rootProject.name = 'eureka-server'
```

### 1.4 — EurekaServerApplication.java

```java
package com.codeWithJeff.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer      // ← This single annotation makes it a Eureka Server
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

### 1.5 — application.yml

```yaml
server:
  port: 8761                        # Standard Eureka port

spring:
  application:
    name: eureka-server             # Service name in the registry

eureka:
  client:
    register-with-eureka: false     # Don't register itself (it IS the registry)
    fetch-registry: false           # Don't fetch registry (it IS the registry)
  server:
    enable-self-preservation: false  # Disable for dev (enable in production)
    eviction-interval-timer-in-ms: 5000  # Check for dead services every 5s (dev)
```

### 1.6 — Start the Eureka Server

```powershell
cd eureka-server
.\gradlew.bat bootRun
```

### 1.7 — Access the Dashboard

Open: **http://localhost:8761**

You'll see the Eureka dashboard with:
- **System Status** — uptime, environment
- **Instances currently registered** — empty for now
- **General Info** — memory, CPUs

---

## Part 2 — Register This Project as a Eureka Client

Now modify your **SampleSpringBootApplication** to register with Eureka.

### 2.1 — Add Dependencies to build.gradle

```groovy
// Add Spring Cloud BOM to dependency management
dependencyManagement {
    imports {
        mavenBom 'com.azure.spring:spring-cloud-azure-dependencies:5.21.0'
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:2024.0.1'   // ← ADD
    }
}

dependencies {
    // ...existing dependencies...

    // Eureka Client — registers this service with Eureka Server
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'   // ← ADD
}
```

Then refresh:
```powershell
.\gradlew.bat build --refresh-dependencies
```

### 2.2 — Add Eureka Config to application.yml

```yaml
spring:
  application:
    name: student-service              # ← This is how other services find you
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:postgres}

server:
  port: 7000

# Eureka client configuration
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/    # Eureka server URL
    register-with-eureka: true          # Register this service (default: true)
    fetch-registry: true                # Fetch list of other services (default: true)
  instance:
    prefer-ip-address: true             # Register with IP instead of hostname
    instance-id: ${spring.application.name}:${server.port}   # Unique instance ID
```

### 2.3 — Enable Discovery Client (Optional in Spring Boot 3)

In Spring Boot 3.x, `@EnableDiscoveryClient` is **auto-configured** — you don't need to add it explicitly if `spring-cloud-starter-netflix-eureka-client` is on the classpath.

But you can add it for clarity:

```java
@SpringBootApplication
@EnableDiscoveryClient           // Optional — auto-configured in Spring Boot 3
public class SampleSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleSpringBootApplication.class, args);
    }
}
```

### 2.4 — Start Both and Verify

1. Start Eureka Server first:
   ```powershell
   cd eureka-server; .\gradlew.bat bootRun
   ```

2. Start your application:
   ```powershell
   cd SampleSpringBootApplication; .\gradlew.bat bootRun
   ```

3. Check the Eureka Dashboard: http://localhost:8761
   - Under **"Instances currently registered with Eureka"**, you should see:
   ```
   Application         AMIs        Availability Zones    Status
   STUDENT-SERVICE     n/a         n/a                   UP(1) - student-service:7000
   ```

### 2.5 — Profile-Specific Eureka Config

You may want to disable Eureka when running locally with H2:

**application-h2.yml** (add at the bottom):
```yaml
# Disable Eureka for local H2 development
eureka:
  client:
    enabled: false
```

**application-postgres.yml** (add at the bottom):
```yaml
# Enable Eureka for deployed environments
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

## Part 3 — Inter-Service Communication

Once services are registered with Eureka, they can call each other **by name** instead of hardcoded URLs.

### 3.1 — Using RestClient (Spring Boot 3.2+, Recommended)

**Create a load-balanced RestClient bean:**

```java
@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced       // ← Enables service name resolution via Eureka
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
```

**Use it in a service:**

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final RestClient.Builder restClientBuilder;

    public StudentResponseDto getStudentForOrder(Long studentId) {
        RestClient restClient = restClientBuilder.build();

        // "student-service" is resolved by Eureka to the actual IP:port
        return restClient.get()
                .uri("http://student-service/api/students/{id}", studentId)
                .retrieve()
                .body(StudentResponseDto.class);
    }
}
```

### 3.2 — Using WebClient (Reactive/Non-blocking)

```java
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final WebClient.Builder webClientBuilder;

    public Mono<StudentResponseDto> getStudentAsync(Long studentId) {
        return webClientBuilder.build()
                .get()
                .uri("http://student-service/api/students/{id}", studentId)
                .retrieve()
                .bodyToMono(StudentResponseDto.class);
    }
}
```

### 3.3 — Using OpenFeign (Declarative, Easiest)

**Add dependency:**
```groovy
implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
```

**Enable Feign:**
```java
@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication { ... }
```

**Create a Feign Client interface:**
```java
// This interface automatically calls student-service via Eureka
@FeignClient(name = "student-service")
public interface StudentClient {

    @GetMapping("/api/students/{id}")
    StudentResponseDto getStudentById(@PathVariable Long id);

    @GetMapping("/api/students")
    List<StudentResponseDto> getAllStudents();
}
```

**Use it like a regular service:**
```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final StudentClient studentClient;    // Injected automatically

    public void processOrder(Long studentId) {
        // This call goes to Eureka → resolves student-service → makes HTTP call
        StudentResponseDto student = studentClient.getStudentById(studentId);
        // ... process the order
    }
}
```

### Comparison — Which to Use?

| Method | Style | Best for |
|---|---|---|
| **RestClient** | Imperative, manual | Simple calls, full control, Spring Boot 3.2+ |
| **WebClient** | Reactive, non-blocking | High-throughput, async operations |
| **OpenFeign** | Declarative, interface-based | Cleanest code, most microservice projects |

---

## Part 4 — Multiple Microservices Example

Here's how a realistic microservices setup looks with Eureka:

```
┌────────────────────────────────────────────────────────────┐
│ Your Laptop (Development)                                  │
│                                                            │
│  ┌─────────────────┐                                       │
│  │ eureka-server    │  ← http://localhost:8761              │
│  │ (port 8761)      │                                      │
│  └────────┬─────────┘                                      │
│           │                                                │
│     ┌─────┼──────────────────┐                             │
│     │     │                  │                             │
│  ┌──┴──────┴───┐  ┌──────────┴───┐  ┌───────────────┐    │
│  │ student-    │  │ order-       │  │ notification- │     │
│  │ service     │  │ service      │  │ service       │     │
│  │ (port 7000) │  │ (port 8080)  │  │ (port 9090)   │    │
│  │             │  │              │  │               │     │
│  │ /api/       │  │ /api/orders  │  │ /api/notify   │    │
│  │  students   │  │              │  │               │     │
│  └─────────────┘  └──────────────┘  └───────────────┘    │
│                                                            │
│  Each is a separate Spring Boot project with:              │
│  - Its own build.gradle                                    │
│  - Its own database (or shared)                            │
│  - spring-cloud-starter-netflix-eureka-client              │
│  - A unique spring.application.name                        │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

**Each service's application.yml needs:**
```yaml
spring:
  application:
    name: <unique-service-name>      # student-service, order-service, etc.

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

## Part 5 — API Gateway with Eureka

In a real microservices architecture, clients don't call services directly. They go through an **API Gateway**:

```
Client → API Gateway (port 8080) → Eureka → student-service (port 7000)
                                           → order-service (port 8081)
                                           → payment-service (port 9090)
```

### Add Spring Cloud Gateway

**Create a new project: `api-gateway/`**

**build.gradle:**
```groovy
dependencyManagement {
    imports {
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:2024.0.1'
    }
}

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
}
```

**application.yml:**
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true                    # Auto-discover routes from Eureka
          lower-case-service-id: true      # Use lowercase service names in URLs

      # Or define explicit routes:
      routes:
        - id: student-service
          uri: lb://student-service        # "lb://" = load-balanced via Eureka
          predicates:
            - Path=/api/students/**

        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**Now clients call:** `http://localhost:8080/api/students` → Gateway routes to `student-service`

---

## 🏥 Health Checks and Monitoring

### Add Spring Boot Actuator

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

**application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics

# Eureka uses actuator health for instance status
eureka:
  instance:
    health-check-url-path: /actuator/health
```

**Don't forget to permit actuator in SecurityConfig:**
```java
.requestMatchers("/actuator/**").permitAll()
```

### Eureka Dashboard Shows Health

On the Eureka dashboard (http://localhost:8761), each service shows:
- **UP** — healthy, accepting traffic
- **DOWN** — health check failed
- **OUT_OF_SERVICE** — manually taken offline

---

## ⚙️ Production Considerations

### 1. Multiple Eureka Servers (High Availability)

In production, run 2-3 Eureka servers that replicate with each other:

**eureka-server-1 (application.yml):**
```yaml
server:
  port: 8761

eureka:
  client:
    service-url:
      defaultZone: http://eureka-server-2:8762/eureka/    # Points to peer
  instance:
    hostname: eureka-server-1
```

**eureka-server-2 (application.yml):**
```yaml
server:
  port: 8762

eureka:
  client:
    service-url:
      defaultZone: http://eureka-server-1:8761/eureka/    # Points to peer
  instance:
    hostname: eureka-server-2
```

**Clients register with both:**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server-1:8761/eureka/,http://eureka-server-2:8762/eureka/
```

### 2. Enable Self-Preservation in Production

```yaml
eureka:
  server:
    enable-self-preservation: true          # Don't remove services during network partitions
    eviction-interval-timer-in-ms: 60000   # Check every 60 seconds
```

### 3. Secure the Eureka Server

Add Spring Security to the Eureka Server:
```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
```

```yaml
spring:
  security:
    user:
      name: eureka-admin
      password: secret-password

# Clients use credentials:
eureka:
  client:
    service-url:
      defaultZone: http://eureka-admin:secret-password@localhost:8761/eureka/
```

---

## 🔧 Troubleshooting Eureka

### Service Not Appearing in Dashboard

| Check | Fix |
|---|---|
| Is Eureka Server running? | Start it first: `http://localhost:8761` |
| Is the client dependency correct? | `spring-cloud-starter-netflix-eureka-client` (not `server`) |
| Is `eureka.client.enabled` set to false? | Remove it or set to `true` |
| Is `spring.application.name` set? | Must be present — this is the service name |
| Is `defaultZone` URL correct? | Must end with `/eureka/` |
| Is Spring Cloud BOM version compatible? | Spring Boot 3.4.x → Spring Cloud 2024.0.x |

### "Connection refused to localhost:8761"

**Cause:** Eureka Server isn't running or is on a different port.

**Fix:**
1. Start the Eureka Server first
2. Verify it's on port 8761
3. Check firewall rules

### Services Keep De-registering

**Cause:** Heartbeat interval too long, or self-preservation kicking in.

**Fix for dev:**
```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 5      # Send heartbeat every 5s (default: 30)
    lease-expiration-duration-in-seconds: 15   # Remove after 15s of no heartbeat (default: 90)
  server:
    enable-self-preservation: false            # Disable for development
```

### Load Balancing Not Working

**Cause:** Missing `@LoadBalanced` annotation on the RestClient/WebClient builder.

**Fix:** Add `@LoadBalanced` to your client bean:
```java
@Bean
@LoadBalanced           // ← Without this, "http://student-service" won't resolve
public RestClient.Builder restClientBuilder() {
    return RestClient.builder();
}
```

---

## 📌 Quick Reference — Annotations & Properties

### Annotations

| Annotation | Where | Purpose |
|---|---|---|
| `@EnableEurekaServer` | Eureka Server main class | Makes the app a Eureka registry server |
| `@EnableDiscoveryClient` | Client main class | Enables service registration (auto-configured in Boot 3) |
| `@EnableFeignClients` | Client main class | Enables Feign declarative HTTP clients |
| `@FeignClient(name = "x")` | Interface | Declares a Feign client for service "x" |
| `@LoadBalanced` | RestClient/WebClient Bean | Enables service name → IP resolution via Eureka |

### Key Properties (Client)

```yaml
spring.application.name: student-service          # Service name in Eureka registry
eureka.client.service-url.defaultZone: http://localhost:8761/eureka/
eureka.client.register-with-eureka: true           # Register with Eureka (default: true)
eureka.client.fetch-registry: true                 # Fetch other services' locations (default: true)
eureka.client.enabled: true                        # Enable/disable Eureka client
eureka.instance.prefer-ip-address: true            # Register IP instead of hostname
eureka.instance.instance-id: ${spring.application.name}:${server.port}
eureka.instance.lease-renewal-interval-in-seconds: 30    # Heartbeat interval
eureka.instance.lease-expiration-duration-in-seconds: 90  # Timeout before removal
```

### Key Properties (Server)

```yaml
server.port: 8761                                           # Standard Eureka port
eureka.client.register-with-eureka: false                   # Server doesn't register itself
eureka.client.fetch-registry: false                         # Server doesn't fetch from itself
eureka.server.enable-self-preservation: false                # Disable for dev
eureka.server.eviction-interval-timer-in-ms: 5000          # How often to check for dead services
```

---

**Document Version:** 1.0
**Created:** April 8, 2026
**Note:** This guide uses Spring Cloud 2024.0.1 compatible with Spring Boot 3.4.5. The Eureka Server is a separate project from your SampleSpringBootApplication.

