# Employee Management System in Spring Boot (Spring Boot + Thymeleaf + Spring Security + JPA + PostgreSQL HRMS)

A production-grade **Spring Boot employee management system (HRMS) with full source code** —
login with **role-based access control (ADMIN / HR / MANAGER / EMPLOYEE)**, employee &
department CRUD with search, pagination and validation, server-rendered **Thymeleaf** UI,
**Spring Data JPA + PostgreSQL** with Flyway migrations, and a complete **Docker Compose** setup.

The full project lives in [`ems/`](ems/README.md) and runs with one command:

```bash
cd ems
docker compose up --build
# open http://localhost:8080 — demo login: admin / Admin@123
```

See [`ems/README.md`](ems/README.md) for features, the RBAC permission matrix,
default credentials, local development, testing (`mvn verify`, 27 tests) and the
project structure.
