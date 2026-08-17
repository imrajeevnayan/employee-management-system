# Employee Management System in Spring Boot — HRMS with Thymeleaf, Spring Security RBAC, Spring Data JPA & PostgreSQL (Full Source Code)

![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6%20RBAC-6DB33F)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3-005F0F)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Tests](https://img.shields.io/badge/tests-27%20passing-brightgreen)

Searching for an **employee management system project in Spring Boot with source code**? Looking for a **Spring Boot + Thymeleaf + Spring Security + JPA CRUD example** with login and role-based access control, a **Spring HRMS / human resource management system on GitHub**, or a production-ready **Spring Boot PostgreSQL Docker Compose template** you can actually deploy? EMS is all of those in one repo: a full-stack, server-side rendered **Spring Boot employee management system (HRMS)** with authentication, admin/HR/manager/employee roles, employee & department CRUD, search, pagination and validation — built with **Spring Boot 3, Thymeleaf, Spring Security, Spring Data JPA (Hibernate), Flyway and PostgreSQL**, fully containerized with **Docker**, and covered by **27 passing integration and unit tests**.

> Everything runs with one command — `docker compose up --build` — and the database is auto-seeded with demo departments, employees and one login account per role so you can explore the RBAC immediately.

---

## Employee Management System Spring Boot — Feature Highlights

- **Login & authentication** — custom Thymeleaf login page, BCrypt password hashing, CSRF protection, session fixation protection, logout, account enable/disable.
- **Role-based access control (RBAC)** — four roles (ADMIN, HR, MANAGER, EMPLOYEE) enforced at the URL, method (`@PreAuthorize`) and service layers.
- **Employee CRUD with server-side validation** — create, view, update, delete employees with Bean Validation, unique email/employee-code checks, auto-generated `EMP-0001` codes and a full audit trail (who created/updated each record and when).
- **Employee directory with search, filters, sorting & pagination** — free-text search across code, name, email and job title; filter by department and employment status; sort by name, hire date or salary — all server-side via JPA `Specification` and `Pageable`.
- **Department management** — full CRUD with unique name/code validation; deletion blocked while employees are assigned; live headcount per department.
- **User & role management (admin panel)** — create user accounts, assign roles, enable/disable accounts, reset passwords, and link accounts 1:1 to employee records.
- **Manager department scoping** — managers can only edit employees in their **own department**, derived from their linked employee record (a favourite interview question, implemented for real).
- **Employee self-service portal** — every user views their own profile and changes their password (current-password verification + strength policy).
- **HR analytics dashboard** — headcount KPIs, employees-by-department distribution chart, recent hires list.
- **Production hygiene** — Flyway versioned schema with `ddl-auto: validate`, JPA auditing, optimistic locking (`@Version`), graceful shutdown, Actuator health probes, custom 403/404/500 error pages, `open-in-view: false`.

## Tech Stack: Spring Boot 3, Thymeleaf, Spring Security, Spring Data JPA, PostgreSQL, Docker

| Layer      | Technology                                            |
|------------|-------------------------------------------------------|
| Runtime    | Java 21, Spring Boot 3.3                              |
| Web / View | Spring MVC, Thymeleaf 3 (+ Spring Security dialect)   |
| Security   | Spring Security 6 — form login, BCrypt, CSRF, method security, RBAC |
| Data       | Spring Data JPA (Hibernate 6), PostgreSQL 16, Flyway migrations |
| UI         | Bootstrap 5.3 + Bootstrap Icons (CDN), custom theme, Thymeleaf layout fragments |
| Build/Run  | Maven, Docker multi-stage image, Docker Compose       |
| Testing    | JUnit 5, Mockito, MockMvc, spring-security-test, H2 in-memory DB |

## Quick Start: Run the Spring Boot HRMS with Docker Compose

```bash
cd ems
docker compose up --build
```

Open **http://localhost:8080** — that's it. The stack boots PostgreSQL 16, runs the Flyway migration, starts the app (non-root container with a health check on `/actuator/health`) and seeds demo data: 4 departments, 10 employees and 4 accounts, one per role.

**Ports:** app on `8080`, PostgreSQL on `127.0.0.1:5432` (localhost only).
**Data:** persisted in the `ems_pgdata` named volume — reset everything with `docker compose down -v` to re-seed from scratch.

## Demo Login Credentials (Seeded Accounts)

| Username    | Password       | Roles     | Linked employee                |
|-------------|----------------|-----------|--------------------------------|
| `admin`     | `Admin@123`    | ADMIN     | — (no employee record)         |
| `hr.kavya`  | `Hr@12345`     | HR        | Kavya Sharma (Human Resources) |
| `mgr.arjun` | `Manager@123`  | MANAGER   | Arjun Mehta (Engineering)      |
| `emp.priya` | `Employee@123` | EMPLOYEE  | Priya Nair (Marketing)         |

The admin password is applied on **first boot only** and can be overridden with the `EMS_ADMIN_PASSWORD` environment variable (already wired through Docker Compose). Change all demo passwords before any real deployment.

## Role-Based Access Control (RBAC) Matrix — Spring Security Example

| Capability                          | ADMIN | HR | MANAGER | EMPLOYEE |
|-------------------------------------|:-----:|:--:|:-------:|:--------:|
| Dashboard                           | ✅    | ✅ | ✅      | ✅       |
| View employee directory & details   | ✅    | ✅ | ✅      | own record only |
| Create employees                    | ✅    | ✅ | ❌      | ❌       |
| Edit employees                      | ✅    | ✅ | own department only | ❌ |
| Delete employees                    | ✅    | ❌ | ❌      | ❌       |
| View departments                    | ✅    | ✅ | ✅      | ❌       |
| Create / edit / delete departments  | ✅    | ✅ | ❌      | ❌       |
| Manage users, roles & passwords     | ✅    | ❌ | ❌      | ❌       |
| Own profile / password change       | ✅    | ✅ | ✅      | ✅       |

RBAC is enforced in **three layers** (so it survives refactoring and makes a great reference implementation):

1. **URL rules** in `SecurityConfig` — `/users/**` requires ADMIN, `/departments/**` allows ADMIN/HR/MANAGER, etc.
2. **Method security** — `@PreAuthorize("hasRole('ADMIN')")` on mutating controller and service methods.
3. **Fine-grained service checks** — `EmployeeService.assertCanView/assertCanManage` implements manager-department scoping and employee self-access; violations render a friendly 403 page.

## Spring Boot Project Structure — Controller, Service, Repository Layers

```
ems/
├── src/main/java/com/ems/
│   ├── config/          # SecurityConfig (RBAC rules), AuditingConfig, DataSeeder
│   ├── domain/          # JPA entities: Employee, Department, User, Role + enums
│   ├── dto/             # Validated form-backing objects + PasswordPolicy
│   ├── exception/       # Domain exceptions + @ControllerAdvice global handlers
│   ├── repository/      # Spring Data repositories + JPA Specifications
│   ├── service/         # Business logic: RBAC scoping, uniqueness, dashboards
│   └── web/             # Controllers (login, dashboard, CRUD, profile)
├── src/main/resources/
│   ├── application.yml  # PostgreSQL, Flyway, Actuator, graceful shutdown
│   ├── db/migration/    # V1__init.sql — versioned schema (ddl-auto=validate)
│   ├── static/css/      # Custom Bootstrap theme
│   └── templates/       # Thymeleaf views: fragments/, employees/, departments/,
│                        #   users/, profile/, dashboard/, error/, login.html
├── src/test/            # 27 tests: security matrix, rendering, services, repositories
├── Dockerfile           # Multi-stage: Maven build → JRE 21 Alpine, non-root, healthcheck
├── docker-compose.yml   # postgres:16 + app with healthchecks and volume
└── pom.xml
```

## Run Spring Boot + PostgreSQL Locally Without Docker

Start only the database, then run the app with Maven:

```bash
docker compose up -d db
mvn spring-boot:run
```

### Configuration reference (environment variables)

| Variable                      | Default                                |
|-------------------------------|----------------------------------------|
| `SPRING_DATASOURCE_URL`       | `jdbc:postgresql://localhost:5432/ems` |
| `SPRING_DATASOURCE_USERNAME`  | `ems`                                  |
| `SPRING_DATASOURCE_PASSWORD`  | `ems` (Docker Compose uses `ems_secret`)|
| `EMS_ADMIN_PASSWORD`          | `Admin@123` (first-boot seed only)     |

## Testing the Employee Management System (JUnit 5, MockMvc, H2)

```bash
mvn verify
```

- **`WebSecurityTests`** — the full RBAC matrix and login flow via MockMvc: unauthenticated redirects, BCrypt login success/failure, HR blocked from `/users`, employees restricted to their own record, managers blocked cross-department, plus Thymeleaf rendering checks for dashboard, lists, detail and form pages.
- **`EmployeeServiceTests`** — business rules as pure Mockito unit tests: duplicate-email rejection, sequential employee codes, manager scoping.
- **`EmployeeRepositoryTests`** — JPA slice tests on in-memory H2: user links, department counts, code ordering.
- **`FlywaySchemaValidationTests`** — applies the real `V1__init.sql` and boots Hibernate with `validate`, proving schema and entities never drift.
- **`EmsApplicationTests`** — full application-context smoke test.

No PostgreSQL needed for tests — they run against H2 in PostgreSQL compatibility mode.

## Database Migrations with Flyway (Versioned PostgreSQL Schema)

The schema is owned by Flyway (`src/main/resources/db/migration/V1__init.sql`) and Hibernate runs with `ddl-auto: validate` — the pattern production teams use instead of auto-generated DDL. Tables: `employees`, `departments`, `users`, `roles`, `user_roles`, with unique constraints (email, username, employee code, department name/code), FK cascade rules and indexes on the columns the directory filters and sorts by.

## Security Best Practices Implemented

- BCrypt-hashed passwords with an enforced strength policy (upper/lower/digit/special, 8–64 chars) on every password-setting surface.
- All state-changing requests are POST + CSRF token (Thymeleaf injects the hidden field automatically); logout is a CSRF-protected POST.
- Self-lockout protection — an admin cannot disable their own account or strip their own ADMIN role.
- Departments with assigned employees cannot be deleted; uniqueness enforced at both service and database levels.
- Optimistic locking (`@Version`) against concurrent edits; JPA auditing records who changed every row and when.
- Stack traces never reach the browser — safe error pages with details logged server-side only.

## Spring Boot Project for Resume & Interviews — What You'll Learn

- How to build a **layered Spring Boot CRUD application** (controller → service → repository) with validated form DTOs.
- How to implement **login and role-based access control with Spring Security 6** — URL rules, `@PreAuthorize`, and custom authorization checks in the service layer.
- How to do **server-side search, filtering, sorting and pagination** with Spring Data JPA Specifications and `Pageable`.
- How to wire **Thymeleaf layouts/fragments with a Spring Security-aware navbar** (`sec:authorize`).
- How to ship **Spring Boot + PostgreSQL with Flyway and Docker Compose**, including health checks and non-root containers.
- How to **test Spring Security rules** with MockMvc and spring-security-test.

## FAQ — Employee Management System in Spring Boot

### How do I run this Spring Boot employee management system?

`docker compose up --build` inside the `ems/` folder, then open http://localhost:8080 and sign in with `admin / Admin@123`. See [Quick Start](#quick-start-run-the-spring-boot-hrms-with-docker-compose).

### What are the default login credentials?

Four seeded accounts — `admin`, `hr.kavya`, `mgr.arjun`, `emp.priya` — listed in the [credentials table](#demo-login-credentials-seeded-accounts). Each demonstrates a different role.

### How is role-based access control implemented in Spring Security here?

Three enforcement layers: URL `requestMatchers` rules in `SecurityConfig`, `@PreAuthorize` method security, and service-level checks for fine-grained rules like managers editing only their own department. See the [RBAC matrix](#role-based-access-control-rbac-matrix--spring-security-example).

### Can I use MySQL or H2 instead of PostgreSQL?

The app targets PostgreSQL 16 via Flyway + `ddl-auto: validate`. Tests already run on H2 in PostgreSQL mode; swapping the driver/JDBC URL and adjusting the migration dialect is all that's needed for MySQL.

### How are passwords stored?

BCrypt hashes (Spring Security's `PasswordEncoder`), never plaintext, with a strength policy enforced on create, reset and change flows.

### Does the project include tests?

Yes — 27 tests covering the security matrix, page rendering, service business rules, repositories and Flyway schema validation. Run them with `mvn verify`; no external database required.

## Troubleshooting

- **`ValidationFailedSchemaManagementError` on boot** — the database schema doesn't match the entities. Run `docker compose down -v` to reset the volume (destroys data) and let Flyway recreate it.
- **First Docker build is slow** — Maven downloads all dependencies inside the build stage; subsequent builds reuse the cached dependency layer.
- **Port conflicts** — change the `8080:8080` / `5432:5432` mappings in `docker-compose.yml`.

## Documentation Site (GitHub Pages)

This repo also ships a search-optimized landing page in [`docs/index.html`](docs/index.html) —
a second indexable page for Google with structured data (JSON-LD `SoftwareApplication` +
`FAQPage`), Open Graph tags, `robots.txt` and `sitemap.xml`, cross-linked to this README.

**To enable GitHub Pages:**

1. Push the repo (with `ems/` as the repository root, so `docs/` sits at `/docs`).
2. Repo **Settings → Pages** → Source: *Deploy from a branch* → Branch: `main`, folder `/docs` → Save.
3. The page goes live at `https://<username>.github.io/ems/` — then replace the
   `your-username` placeholder in [`docs/index.html`](docs/index.html),
   [`docs/robots.txt`](docs/robots.txt) and [`docs/sitemap.xml`](docs/sitemap.xml)
   with your actual GitHub username so canonical URLs and sitemap resolve correctly.

---

**Suggested repository topics** (GitHub indexes these for search): `spring-boot` · `employee-management-system` · `hrms` · `spring-security` · `thymeleaf` · `spring-data-jpa` · `postgresql` · `docker-compose` · `rbac` · `java` · `flyway` · `human-resource-management`
