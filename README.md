# E-Commerce Backend API

REST API (`/api/v1`) for a Flutter mobile app, a web storefront and an admin dashboard. It is built as a
layered Spring MVC application with Java 21, Spring Boot 4.1, PostgreSQL, Redis and S3 storage.

**Status:** under construction, phase by phase. See [docs/DESIGN.md](docs/DESIGN.md) for the design, the
ERD, the delivery plan and the report of each finished phase. Phases 1, 2 and 4 are complete: setup and
authentication ([Auth API](docs/api/auth.md)). Catalog, cart and order endpoints follow phase by phase.

| | |
|---|---|
| **Stack** | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, Actuator, Cache), Maven wrapper |
| **Data** | PostgreSQL 17 with Flyway migrations, Redis 7.4, S3-compatible storage (AWS S3 / RustFS) |
| **Quality** | JUnit 5, Mockito, Testcontainers |
| **Docs** | Swagger UI at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs` |

---

## Run it

**Prerequisites:** JDK 21 and Docker Desktop (running).

### Everything in Docker

```bash
docker compose up -d --build
docker compose logs -f api
```

| Service | Address |
|---------|---------|
| API | <http://localhost:8080> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| Health | <http://localhost:8080/actuator/health> |
| Storage console (RustFS) | <http://localhost:9001> (user `ecommerce`, password `ecommerce-local-secret`) |

The defaults in `docker-compose.yml` are for local use only. For anything else, copy `.env.example` to
`.env` and set real passwords. Stop the stack with `docker compose down`; add `-v` to also delete its data.

### API from the IDE, infrastructure in Docker

```bash
docker compose up -d postgres redis storage
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run   (dev profile)
```

In IntelliJ, use the **main** run configuration. The `dev` defaults point to the containers: PostgreSQL on
`localhost:5433` and Redis on `localhost:6379`, both published on localhost only.

The first start applies the Flyway migrations and creates the roles and an administrator
(`admin` / `Admin@123`, from `ADMIN_*`).

---

## Test

```bash
./mvnw test
```

Docker must be running. The integration tests start real PostgreSQL and Redis containers.

| Test | Checks |
|------|--------|
| `PlatformIntegrationTest` | Flyway + schema validation, seed data, health, OpenAPI, JSON error envelope, CORS, Redis cache, money format |
| `CheckoutOnPostgresIntegrationTest` | Checkout with row locks, stock reservation and return, payment, error codes |

---

## API conventions

Every response has the same envelope:

```json
{ "success": true, "data": { "id": 42 }, "timestamp": "2026-11-11T06:40:48Z" }
```

```json
{
  "success": false,
  "error": { "code": "UNAUTHORIZED", "message": "Authentication is required", "path": "/api/v1/orders", "traceId": "3ed44f28" },
  "timestamp": "2026-11-11T06:40:48Z"
}
```

* `error.code` is stable; clients branch on it. The list is in `common/exception/ErrorCode.java`.
* Money is a string with two decimals (`"19.90"`); times are UTC ISO-8601.
* Lists take `page`, `size` (max 100) and `sort=field,asc|desc`.
* Every response carries an `X-Request-Id` header; the same id is in `error.traceId` and in the server logs.

---

## Project structure (layered Spring MVC)

```
src/main/java/com/example/ecommerce
├── EcommerceApplication.java
├── controller/      REST controllers (/api/v1/...): validate input, call one service, wrap the result in ApiResponse
├── service/         Business logic interfaces (AuthService, UserAccountService, OrderService, ...)
│   └── impl/        Their implementations; transactions live here
├── repository/      Spring Data JPA repositories (+ ProductSpecifications for dynamic search)
├── model/           JPA entities (User, Role, Product, Order, RefreshToken, ...) and BaseEntity
│   └── enums/       RoleName, OrderStatus, PaymentMethod, PaymentStatus, AuditAction
├── dto/
│   ├── request/     Request bodies with Bean Validation rules
│   └── response/    Response records, ApiResponse / ApiError envelope, PageResponse
├── mapper/          Entity -> response DTO
├── security/        SecurityConfig, JWT service and filter, rate limiting, UserPrincipal
├── config/          AppProperties, Jackson, OpenAPI, Redis cache, S3 storage, request logging, data seeding
├── exception/       ErrorCode, business exceptions, GlobalExceptionHandler
└── util/            Shared constants (password rules, cart limits)
src/main/resources
├── application.yml  Profiles: dev (default), test, prod
└── db/migration/    Flyway scripts (V1__baseline.sql, V2__refresh_tokens.sql, ...)
```

Request flow: `controller` → `service` → `repository` → database; entities (`model`) never leave the
service layer, controllers only see `dto` records.

---

## Configuration

All settings are in `application.yml` and can be overridden by environment variables:

| Variable | Default (dev) | Purpose |
|----------|---------------|---------|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5433`, `ecommerce_db` | PostgreSQL |
| `DB_USERNAME`, `DB_PASSWORD` | `ecommerce` / `ecommerce-local` | PostgreSQL credentials |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | `localhost`, `6379`, `redis-local` | Redis |
| `S3_ENDPOINT`, `S3_BUCKET`, `S3_REGION` | `http://localhost:9000`, `ecommerce-media`, `us-east-1` | Object storage (leave `S3_ENDPOINT` empty for AWS S3) |
| `S3_ACCESS_KEY`, `S3_SECRET_KEY` | local keys | Storage credentials (AWS: leave empty to use the IAM role) |
| `S3_PUBLIC_BASE_URL` | `http://localhost:9000/ecommerce-media` | Base URL clients use to load images |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,5173,4200` | Browser origins of the web clients |
| `JWT_SECRET` | dev key | **Base64 HMAC key, at least 256 bits** (`openssl rand -base64 64`); required in prod |
| `JWT_ACCESS_TOKEN_TTL`, `JWT_REFRESH_TOKEN_TTL` | `15m`, `30d` | Token lifetimes |
| `RATE_LIMIT_ENABLED` | `true` | Per-IP limits on the public auth endpoints |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin` / `admin@shop.local` / `Admin@123` | Bootstrap super administrator |
| `SHOP_CURRENCY`, `SHOP_SHIPPING_FEE`, `SHOP_FREE_SHIPPING_THRESHOLD` | `USD`, `2.00`, `50.00` | Store settings |
| `APP_TIMEZONE` | server zone | Store time zone, e.g. `Asia/Phnom_Penh` |
| `SWAGGER_ENABLED` | on (dev), off (prod) | Expose Swagger UI in production |

The `prod` profile has no defaults for secrets: the database, CORS origins, storage URL and admin password
must come from the environment.
