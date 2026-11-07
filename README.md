# School Attendance Management System (Spring Boot)

A REST API that lets administrators, teachers and students manage and track daily school attendance:
students, teachers, classes, subjects, academic years and semesters, roll calls, reports (Excel and PDF) and
role-based dashboards.

| | |
|---|---|
| **Stack** | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, Actuator, DevTools), Lombok, Maven |
| **Database** | MySQL 8 (H2 in MySQL mode for automated tests) |
| **Auth** | JWT access and refresh tokens (jjwt), BCrypt, role-based access control plus per-row ownership checks |
| **Docs** | Swagger UI at `/swagger-ui.html`, a [Postman collection](postman/), [API reference](docs/API.md), [ERD](docs/ERD.md) |
| **Reports** | Apache POI (Excel), OpenPDF (PDF) |

---

## Quick start (local)

**Prerequisites:** JDK 21 and MySQL 8 running on `localhost:3306`. Maven is not required; use the wrapper.

```bash
# 1. Optional: create the schema yourself in MySQL Workbench / CLI
mysql -u root -p < src/main/resources/db/schema.sql
#    (otherwise the dev profile creates and updates the tables automatically)

# 2. Run (dev profile is the default)
./mvnw spring-boot:run                      # Windows: mvnw.cmd spring-boot:run
#    Override DB credentials if they are not root/root:
DB_USERNAME=me DB_PASSWORD=secret ./mvnw spring-boot:run
```

* API: <http://localhost:8080>
* Swagger UI: <http://localhost:8080/swagger-ui.html>. Click **Authorize** and paste an `accessToken`.
* Health: <http://localhost:8080/actuator/health>
* First start creates the roles and an admin account: **`admin` / `Admin@123`**. Change it immediately with
  `POST /api/auth/change-password`.
* Optional demo data: run `src/main/resources/db/sample-data.sql` after the first start.

### Typical first workflow

1. `POST /api/auth/login` as admin.
2. `POST /api/academic-years` and `POST /api/semesters` (mark them `current: true`).
3. `POST /api/subjects`, `POST /api/teachers`, `POST /api/classes` (with `homeroomTeacherId`).
4. `POST /api/students` (with `classId`), or `POST /api/classes/{id}/students`.
5. `POST /api/teachers/{id}/assignments` to let other teachers take attendance in a class.
6. `POST /api/auth/register` to create login accounts linked to `teacherId` or `studentId`.
7. The teacher logs in and calls `POST /api/attendance/bulk` for the daily roll call.
8. Use `GET /api/reports/...` and `GET /api/dashboard/{admin|teacher|student}`.

---

## Configuration

All settings are in `src/main/resources/application.properties` and can be overridden with environment variables.

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `3306`, `school_attendance_db` | MySQL location |
| `DB_USERNAME`, `DB_PASSWORD` | `root` / `root` (dev only) | MySQL credentials |
| `JPA_DDL_AUTO` | `update` (dev), `validate` (prod) | Hibernate schema mode |
| `JWT_SECRET` | dev key | **Base64 HMAC key of at least 256 bits.** Generate with `openssl rand -base64 64` |
| `JWT_ACCESS_EXPIRATION_MINUTES` | `60` | Access token lifetime |
| `JWT_REFRESH_EXPIRATION_DAYS` | `7` | Refresh token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,5173,4200` | Front-end origins (comma-separated) |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin` / `admin@school.local` / `Admin@123` | Bootstrap admin (created only if missing) |
| `PASSWORD_RESET_URL` | `http://localhost:3000/reset-password` | Front-end page that receives `?token=` |
| `APP_TIMEZONE` | the server's zone | School time zone, for example `Asia/Phnom_Penh`. It defines "today" for dashboards, defaults and the edit window |
| `SWAGGER_ENABLED` | `true` (dev), `false` (prod) | Expose Swagger UI and the API docs |
| `app.attendance.count-late-as-present` | `true` | Whether LATE counts toward the attendance percentage |
| `app.attendance.teacher-edit-window-days` | `7` | How long teachers may edit past attendance (admins: always) |

Profiles: `dev` (default; verbose logging, auto DDL), `prod` (every secret from the environment, `validate`,
Swagger off, graceful shutdown), and `test` (H2, used by the test suite).

---

## Project structure

```
src/main/java/org/example/panhabackend
├── PanhaBackEndApplication.java      Entry point (also applies APP_TIMEZONE)
├── config/        AppProperties (typed app.*), DataInitializer (roles + admin), OpenApiConfig, RequestLoggingFilter
├── security/      SecurityConfig, JwtService, JwtAuthenticationFilter, UserPrincipal, CustomUserDetailsService,
│                  RestAuthenticationEntryPoint (401 JSON), RestAccessDeniedHandler (403 JSON),
│                  SecurityUtils, AccessControlService (row-level rules)
├── controller/    Auth, User, Student, Teacher, SchoolClass, Subject, AcademicYear, Semester,
│                  Attendance, Report, Dashboard, AuditLog
├── service/       Interfaces (one per module) + PasswordResetNotifier
│   └── impl/      Implementations, ExportServiceImpl (Excel/PDF), LoggingPasswordResetNotifier
├── repository/    Spring Data JPA repositories
│   ├── projection/  StatusCount, StudentStatusCount, ClassCount (aggregate query results)
│   └── spec/        JPA Specifications for dynamic search
├── entity/        BaseEntity + 14 tables' entities
├── dto/           Request/response records grouped by module (auth, user, student, teacher, schoolclass,
│                  subject, academic, attendance, report, dashboard, audit, common)
├── mapper/        Entity <-> DTO mappers (entities are never exposed by the API)
├── exception/     Custom exceptions, ErrorResponse, GlobalExceptionHandler
└── enums/         RoleName, Gender, StudentStatus, TeacherStatus, AttendanceStatus, ReportPeriod, ReportType, ExportFormat, AuditAction
src/main/resources
├── application.properties / -dev / -prod
└── db/schema.sql, db/sample-data.sql
src/test/...       Unit tests + end-to-end integration test (H2)
docs/              API.md (all endpoints with JSON examples), ERD.md (diagram + relationships)
postman/           Collection, local environment, generator script
Dockerfile, docker-compose.yml, .env.example
```

---

## Development phases and where to find them

| Phase | What was built | Key files |
|-------|----------------|-----------|
| 1. Project initialization | Dependencies and profile configuration | `pom.xml`, `application*.properties`, `AppProperties` |
| 2. Database design | 14 normalized tables, keys, unique constraints, indexes, ERD | `entity/*`, `db/schema.sql`, `docs/ERD.md` |
| 3. Repositories and services | Repositories, specifications, projections, service interfaces and implementations, transactions | `repository/**`, `service/**` |
| 4. Authentication and JWT | Login, register (admin-only), refresh, logout with revocation, change, forgot and reset password, RBAC | `security/*`, `AuthServiceImpl`, `AuthController` |
| 5. Core management | Students, teachers, classes (enrollment, homeroom), subjects, academic years and semesters, assignments | `*ServiceImpl`, `*Controller` |
| 6. Attendance | Single and bulk recording, duplicate prevention, edit window, filters, sessions | `AttendanceServiceImpl`, `AttendanceController` |
| 7. Dashboards and reports | Daily, weekly, monthly, semester and custom reports, absent and late lists, percentage, Excel and PDF export, 3 dashboards | `ReportServiceImpl`, `ExportServiceImpl`, `DashboardServiceImpl` |
| 8. Swagger and Postman | OpenAPI with a bearer scheme, 91-request collection that captures tokens and ids | `OpenApiConfig`, `postman/` |
| 9. Testing | Unit tests (JWT, percentage, report ranges, attendance rules) and an end-to-end flow through MockMvc and H2 | `src/test/**` |
| 10. Deployment | Multi-stage Dockerfile (non-root), Compose with MySQL and health checks, prod profile | `Dockerfile`, `docker-compose.yml`, `.env.example` |

---

## Key design decisions

* **Duplicate prevention.** A roll call is an `attendance_session` that is unique on (class, date, period). A
  record is unique on (session, student). The service checks first and returns a readable `409` listing the
  affected students; the database constraints are the final guard against concurrent requests.
* **Bulk is all or nothing.** `POST /api/attendance/bulk` validates every student (exists, active, in the
  class, not already recorded, not repeated) before saving anything, in a single transaction.
* **Two layers of authorization.** URL and `@PreAuthorize` role checks, plus `AccessControlService` for
  ownership: teachers may only act on the classes they are assigned to or are homeroom teacher of, and
  students only see their own records. List endpoints are scoped automatically, so a student calling
  `GET /api/attendance` only ever sees their own rows.
* **Revocable stateless JWTs.** Each user has a `token_version` that is embedded in tokens. Logout, password
  change or reset, disabling an account and role changes all increment it, which invalidates every
  outstanding token without a token table.
* **No sensitive data in responses.** Only DTOs are returned. Passwords are stored as BCrypt hashes, reset
  tokens as SHA-256 hashes. Request bodies are never logged.
* **History is protected.** Students, teachers, classes and semesters with attendance history cannot be
  deleted (`409`); deactivate them instead. Every create, update, delete, login and password event is written
  to `audit_logs` with the user and IP address.
* **Attendance percentage** = (Present / Total recorded sessions) x 100, with LATE counted as present by
  default (configurable). With one roll call per day, the total equals the number of school days.

---

## Testing

```bash
./mvnw test
```

There are 24 tests and they need no MySQL (they use H2 in MySQL mode):

* `AttendanceStatsTest`: the percentage formula, rounding, and the LATE-as-present switch.
* `JwtServiceTest`: token round trip, refresh-versus-access type check, tampering, weak secrets.
* `AttendanceServiceImplTest` (Mockito): duplicate rejection, wrong class, unassigned teacher, bulk duplicates.
* `ReportRangeTest`: daily, weekly, monthly and custom date windows and their validation.
* `AttendanceFlowIntegrationTest`: the real HTTP stack. It covers login, the 401 and 403 JSON errors,
  validation errors, admin setup, account linking, bulk attendance, duplicate `409`, the teacher edit, student
  scoping, reports, Excel and PDF export, all three dashboards, refusal to delete a student with history, and
  logout revocation.

### Postman

Import `postman/School-Attendance-API.postman_collection.json` and
`postman/School-Attendance-Local.postman_environment.json`, select the environment, then run folders **01
to 11** in order on a fresh database. Login requests store the tokens, and create requests store the ids.
Folder **12** is destructive (deletes, password changes, logout); run it last, on purpose.
After editing requests, regenerate the collection with `node postman/generate-collection.js postman`.

---

## Deployment

### Docker Compose (MySQL and API)

```bash
cp .env.example .env        # set real passwords, JWT_SECRET (openssl rand -base64 64), CORS origins, APP_TIMEZONE
docker compose up -d --build
docker compose logs -f api
```

On the first start of an empty volume, MySQL runs `db/schema.sql`, which creates `school_attendance_db`, and
the API starts with `ddl-auto=validate`. Keep `DB_NAME=school_attendance_db`, or change the database name at
the top of `schema.sql` to match.

### Plain JAR

```bash
./mvnw clean package            # runs the tests, produces target/panha-back-end-0.0.1-SNAPSHOT.jar
SPRING_PROFILES_ACTIVE=prod DB_HOST=... DB_NAME=school_attendance_db DB_USERNAME=... DB_PASSWORD=... \
JWT_SECRET=... ADMIN_PASSWORD=... APP_TIMEZONE=Asia/Phnom_Penh \
java -jar target/panha-back-end-0.0.1-SNAPSHOT.jar
```

### Production checklist

* [ ] A unique `JWT_SECRET` of at least 256 bits; strong DB and admin passwords; the admin password changed after first login.
* [ ] HTTPS terminated at a reverse proxy (Nginx, a load balancer). Forward `X-Forwarded-For` so audit IPs are correct.
* [ ] `CORS_ALLOWED_ORIGINS` limited to the real front-end domain.
* [ ] `APP_TIMEZONE` set to the school's zone.
* [ ] Replace `LoggingPasswordResetNotifier` with an e-mail implementation (for example `spring-boot-starter-mail`).
* [ ] Regular MySQL backups (`mysqldump`) of the `mysql-data` volume.
* [ ] For future schema changes, adopt Flyway or Liquibase, starting from `db/schema.sql` as the baseline.
