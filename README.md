# Fish — Kotlin/Spring Boot Backend Template

A production-ready starter template for a Kotlin + Spring Boot REST API: JWT authentication
with refresh-token rotation, role-based authorization, PostgreSQL + Flyway migrations, request
validation, structured error handling, OpenAPI docs, and a test suite (unit + Testcontainers
integration tests with JaCoCo coverage) wired into GitHub Actions CI.

Use it as a `git init`-and-go base for a new service, or as a reference for how these pieces fit
together in a Kotlin project.

## Features

- **Auth**: register/login/logout/refresh with access tokens (JWT) + rotating refresh tokens
  stored server-side, delivered via an `HttpOnly` cookie.
- **Authorization**: Spring Security with method-level `@PreAuthorize` (role- and
  ownership-based, e.g. "admin or self").
- **Users**: CRUD with pagination, filtering (JPA Specifications), and validation.
- **Database**: PostgreSQL via Spring Data JPA, schema managed with Flyway migrations.
- **Errors**: centralized exception handling with a consistent `ApiError` response shape.
- **Docs**: OpenAPI/Swagger UI via springdoc.
- **Ops**: Spring Boot Actuator with a custom DB health indicator, readiness/liveness probes.
- **Quality**: ktlint for formatting/linting, JaCoCo for coverage, CI on every push/PR.
- **Containerized**: multi-stage-free `Dockerfile` + `docker-compose.yml` (app + Postgres).

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.2 |
| Framework | Spring Boot 4 (Web, Security, Validation, Data JPA, Actuator) |
| Build | Gradle (Kotlin DSL), toolchain on JDK 24 |
| Database | PostgreSQL, Flyway migrations |
| Auth | JJWT (JWT access tokens) + DB-backed refresh tokens |
| Testing | JUnit 5, MockK, Spring MockMvc, Testcontainers (Postgres) |
| Coverage | JaCoCo |
| Lint | ktlint (via `org.jlleitschuh.gradle.ktlint`) |
| CI | GitHub Actions |

## Using This as a Template

1. Click **"Use this template"** on GitHub (or `git clone` + re-init `.git` yourself).
2. Rename the project: update `rootProject.name` in `settings.gradle.kts`, the `group`/`version`
   in `build.gradle.kts`, and the `com.picoding.fish` package if you want a different namespace
   (rename the `src/main/kotlin/com/picoding/fish` and `src/test/kotlin/com/picoding/fish`
   directories, and the package declarations inside).
3. Copy `.env` (see [Configuration](#configuration) below) and fill in real values — it's
   git-ignored, so nothing you put there gets committed.
4. Replace `src/main/resources/db/migration/V1__init.sql` with your own schema, or add new
   `V2__*.sql`, `V3__*.sql`, ... migrations on top of it.
5. Swap out the `User`/`Auth` domain for your own entities, or build alongside it — the
   `database` / `services` / `api` package split is meant to generalize.

## Repository Map

```
.
├── build.gradle.kts              Gradle build: dependencies, ktlint, JaCoCo config
├── settings.gradle.kts           Root project name
├── Dockerfile                    App image (Amazon Corretto 24, builds the jar in-image)
├── docker-compose.yml            App + Postgres for local/full-stack runs
├── .env                          Local secrets/config for docker-compose (git-ignored)
├── docs/
│   └── openapi (1).yml           Exported OpenAPI spec
├── .github/workflows/ci.yml      CI: ktlint + tests/coverage on push & PR
└── src/
    ├── main/
    │   ├── kotlin/com/picoding/fish/
    │   │   ├── FishApplication.kt          Entry point
    │   │   ├── actions/                    Startup actions (e.g. admin user bootstrap)
    │   │   ├── api/
    │   │   │   ├── controllers/            REST controllers (Auth, User)
    │   │   │   ├── exceptions/             AppException hierarchy + global handlers
    │   │   │   ├── health/                 Custom Actuator health indicators
    │   │   │   └── utils/
    │   │   │       ├── cookie/             Refresh-token cookie helper
    │   │   │       └── security/           JWT filter, entry point, SecurityConfig, principal
    │   │   ├── core/
    │   │   │   ├── Settings.kt             Typed `app.*` configuration properties
    │   │   │   ├── mappers/                Entity <-> DTO mapping
    │   │   │   ├── schemas/                Request/response DTOs (user, token)
    │   │   │   ├── specification/          Generic JPA Specification helpers
    │   │   │   ├── utils/                  HashEncoder, PageResponse, etc.
    │   │   │   └── validation/             Custom bean validation constraints
    │   │   └── database/
    │   │       ├── models/                 JPA entities (BaseEntity, User, RefreshToken)
    │   │       └── repositories/           Spring Data repositories + specifications
    │   └── resources/
    │       ├── application.yaml            Base Spring config (env-var driven)
    │       └── db/migration/               Flyway SQL migrations
    └── test/
        ├── kotlin/com/picoding/fish/
        │   ├── FishApplicationTests.kt     Context-load smoke test
        │   ├── api/controllers/            *IT.kt — MockMvc + Testcontainers integration tests
        │   ├── core/utils/                 Unit tests for core utilities
        │   ├── services/                   Unit tests for services (MockK)
        │   └── support/                    Shared test base classes/utilities
        └── resources/application-test.yaml Test profile overrides
```

## Configuration

The app is configured entirely through environment variables (see `application.yaml`); a local
`.env` (git-ignored) is used by `docker-compose.yml`:

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | JDBC connection to PostgreSQL |
| `APP_SECURITY_JWT_SECRET` | Base64 secret used to sign JWT access tokens |
| `APP_SECURITY_ACCESS_TOKEN_EXPIRATION` | Access token TTL (default `15m`) |
| `APP_SECURITY_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL (default `30d`) |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD`, `APP_ADMIN_FULLNAME` | Bootstrap admin account created on startup |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed CORS origins |

Running tests does **not** require any of these to be set — the `test` profile
(`application-test.yaml`) supplies its own values, and integration tests get a real Postgres
instance from Testcontainers automatically.

## Common Commands

All commands use the Gradle wrapper, so no local Gradle install is required. On Windows use
`gradlew.bat`, on macOS/Linux use `./gradlew`.

```powershell
# Run the app locally (needs a reachable Postgres + env vars from .env)
.\gradlew.bat bootRun

# Build a runnable jar (build/libs/fish-0.0.1-SNAPSHOT.jar)
.\gradlew.bat build

# Run all tests (unit + Testcontainers integration tests, needs Docker running)
.\gradlew.bat test

# Run one test class or method
.\gradlew.bat test --tests "com.picoding.fish.services.JWTServiceTest"

# Run tests and generate a JaCoCo coverage report
# (this already runs automatically after `test` — report lands in build/reports/jacoco/test/html)
.\gradlew.bat test jacocoTestReport

# Check formatting/lint
.\gradlew.bat ktlintCheck

# Auto-fix formatting issues
.\gradlew.bat ktlintFormat

# Run the full stack (app + Postgres) in Docker
docker compose up --build
```

## API Overview

Base path: `/api/v1` (set via `server.servlet.context-path`).

- `POST /auth/register` — create an account, returns access token + sets refresh-token cookie
- `POST /auth/login` — authenticate, returns access token + sets refresh-token cookie
- `POST /auth/refresh` — rotate the refresh token, returns a new access token
- `POST /auth/logout` — revoke the current refresh token
- `GET /users` — list users, paginated/filterable (admin only)
- `POST /users` — create a user (admin only)
- `GET /users/me`, `PUT /users/me` — read/update the current user
- `GET /users/{id}`, `PUT /users/{id}`, `DELETE /users/{id}` — admin, or self for GET/PUT

Interactive docs are served by springdoc once the app is running (Swagger UI at
`/api/v1/swagger-ui.html`); a static export lives in `docs/openapi (1).yml`.

## CI

`.github/workflows/ci.yml` runs on every push and PR to `main`:

- **lint** — `ktlintCheck`
- **test** — `test` + `jacocoTestReport`, with JUnit results and the JaCoCo report uploaded as
  build artifacts (Docker is available out of the box on the `ubuntu-latest` runner, so
  Testcontainers-based integration tests run without extra setup)

## License

Apache License 2.0 — see [LICENSE](LICENSE).
