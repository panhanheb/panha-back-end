# Panha Shop (Spring Boot MVC)

A server-rendered online store. Customers sign up, browse products, fill a cart, check out, pay (simulated) and
track their orders. Administrators manage the catalog, fulfil orders and manage accounts in a back office.

| | |
|---|---|
| **Stack** | Java 21, Spring Boot 4.1 (Web MVC, Thymeleaf, Data JPA, Security, Validation, Actuator), Lombok, Maven |
| **Database** | MySQL 8 (PostgreSQL optional for local development, H2 for automated tests) |
| **Sign-in** | Form login with server sessions, BCrypt passwords, CSRF protection, roles `ADMIN` and `CUSTOMER` |
| **Payments** | Simulated (no external gateway): card, bank transfer, cash on delivery |

---

## Quick start (local)

**Prerequisites:** JDK 21 and MySQL 8 on `localhost:3306`. Maven is not required; use the wrapper.

```bash
./mvnw spring-boot:run                      # Windows: mvnw.cmd spring-boot:run
# DB credentials other than root/root:
DB_USERNAME=me DB_PASSWORD=secret ./mvnw spring-boot:run
```

Open <http://localhost:8080>.

* First start creates the roles and an administrator: **`admin` / `Admin@123`**. Sign in and change it under
  **My account**.
* Customers create their own accounts with **Create account**.
* Optional demo catalog (3 categories, 8 products): run `src/main/resources/db/sample-data.sql` after the first start.

**PostgreSQL instead of MySQL:** create the database once with `psql -U postgres -c "CREATE DATABASE ecommerce_db"`,
then run with the `dev,postgres` profiles and your password, e.g.
`SPRING_PROFILES_ACTIVE=dev,postgres DB_PASSWORD=yourpass ./mvnw spring-boot:run`. The IntelliJ run configuration
`main` already uses these profiles.

---

## Pages

| Who | Page | URL |
|-----|------|-----|
| Everyone | Product list with search, category, price and stock filters, sorting | `/`, `/products` |
| Everyone | Product details | `/products/{id}` |
| Everyone | Sign in, create account, forgot / reset password | `/login`, `/register`, `/forgot-password`, `/reset-password?token=` |
| Signed in | Profile and password change | `/account` |
| Customer | Cart (change quantities, remove, empty) | `/cart` |
| Customer | Checkout (delivery address, payment method) | `/checkout` |
| Customer | My orders, order details, payment, cancellation | `/orders`, `/orders/{id}` |
| Admin | Orders: search, ship, deliver, cancel | `/admin/orders` |
| Admin | Products: create, edit, hide, restock, delete | `/admin/products` |
| Admin | Categories | `/admin/categories` |
| Admin | Users: enable/disable, roles, password reset, delete | `/admin/users` |
| Admin | Audit log | `/admin/audit-logs` |

### Order lifecycle

```
            pay (customer)           ship (admin)            deliver (admin)
PENDING ─────────────────────► PAID ───────────────► SHIPPED ─────────────────► DELIVERED
   │  └──────── ship (admin, cash on delivery only) ────►┘   (COD is marked PAID on delivery)
   │                            │
   └──── cancel (customer or admin) ───► CANCELLED   (stock restored; PAID orders become REFUNDED)
```

* Checkout reserves stock immediately. Cancelling returns it.
* Simulated card payment: any 12-19 digit number succeeds, except `4000 0000 0000 0002`, which is always
  declined. Card numbers are never stored.

---

## Configuration

Settings are in `src/main/resources/application.properties` and can be overridden with environment variables.

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `3306`, `ecommerce_db` | Database location |
| `DB_USERNAME`, `DB_PASSWORD` | `root` / `root` (dev only) | Database credentials |
| `JPA_DDL_AUTO` | `update` (dev), `validate` (prod) | Hibernate schema mode |
| `SESSION_TIMEOUT` | `30m` | Idle time before a signed-in session ends |
| `SESSION_COOKIE_SECURE` | `true` in prod | Send the session cookie over HTTPS only. Set `false` when serving plain HTTP |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin` / `admin@shop.local` / `Admin@123` | Bootstrap admin (created only if missing) |
| `PASSWORD_RESET_URL` | `http://localhost:8080/reset-password` | Public address of the reset page, used in reset links |
| `APP_TIMEZONE` | the server's zone | Store time zone, e.g. `Asia/Phnom_Penh` |
| `SHOP_CURRENCY` | `USD` | Currency shown on prices |
| `SHOP_SHIPPING_FEE` | `2.00` | Flat shipping fee per order |
| `SHOP_FREE_SHIPPING_THRESHOLD` | `50.00` | Subtotal from which shipping is free (`0` = never free) |

Profiles: `dev` (default; template reloading, verbose logging, auto DDL), `prod` (secrets from the environment,
`validate`, secure cookie, graceful shutdown), `postgres` (local PostgreSQL) and `test` (H2, used by the tests).

---

## Project structure (MVC)

```
src/main/java/org/example/panhabackend
├── controller/        Controllers: Shop, Auth, Account, Cart, Order, ErrorPage
│   └── admin/         Back office: AdminOrder, AdminProduct, AdminCategory, AdminUser, AdminAudit
├── web/               GlobalModelAttributes (header data), FormValidation, ViewFormat (@fmt), Redirects
│   └── form/          Form-backing objects bound to the HTML forms
├── service/ + impl/   Business logic (pricing, cart, checkout, payment, fulfilment, accounts, audit)
├── repository/        Spring Data JPA repositories, ProductSpecifications (product search)
├── entity/  enums/    JPA entities and enums (the model)
├── dto/  mapper/      Records passed from services to views, entity -> record mappers
├── security/          SecurityConfig (form login), LoginSuccessHandler, SessionValidationFilter, UserPrincipal
├── exception/         Business exceptions, GlobalExceptionHandler (error pages and flash messages)
└── config/            AppProperties, DataInitializer, RequestLoggingFilter
src/main/resources
├── templates/         Thymeleaf views: shop/, auth/, account/, cart/, orders/, admin/, fragments/, error.html
├── static/css/app.css Stylesheet
└── db/                schema.sql (MySQL), sample-data.sql
```

Request flow: a controller reads the request, calls a service, puts the result in the model and returns a
template name. Form submissions follow Post/Redirect/Get: on success the controller redirects and passes the
confirmation as a flash message; on invalid input it shows the same form again with the errors next to the fields.

---

## Key design decisions

* **No overselling.** Checkout locks the product rows (`SELECT ... FOR UPDATE`, in id order to avoid deadlocks)
  before reading stock, then deducts it in the same transaction.
* **All or nothing checkout.** Every cart line is checked before anything is saved; problems are listed together.
* **Orders are snapshots.** Product name, SKU, unit price and the delivery address are copied into the order.
* **Concurrent changes.** `orders.version` is an optimistic lock, so a simultaneous pay and cancel cannot both win.
* **Ownership.** Customers only see their own orders; other order ids show "not found".
* **Sessions end when the account changes.** Disabling an account, changing its roles or resetting/changing its
  password ends all of its sessions on their next request (`users.token_version`, `SessionValidationFilter`).
* **Safe forms.** Every form carries a CSRF token. Input rules are declared once on the service request records
  and reused by the web forms. Error pages never show stack traces.
* **History is protected.** Ordered products, users with orders and categories with products cannot be deleted
  (hide or disable them instead). Every change, payment, sign-in and password event goes to `audit_logs`.

---

## Testing

```bash
./mvnw test
```

`ShopWebFlowTest` drives the real security filter chain, controllers, services and templates on H2: access
rules and CSRF, sign-up validation, the full card purchase with a declined test card and fulfilment, cash on
delivery with cancellation and stock return, order ownership, ending sessions of disabled accounts, password
change, and a check that every page renders.

---

## Deployment

### Docker Compose (MySQL and the web app)

```bash
cp .env.example .env        # set real passwords; SESSION_COOKIE_SECURE=true once served over HTTPS
docker compose up -d --build
docker compose logs -f web
```

The app container is called `nhacg1`. On the first start of an empty volume MySQL runs `db/schema.sql`, which
creates `ecommerce_db`; the app starts with `ddl-auto=validate`.

### Production checklist

* [ ] Strong DB and admin passwords; change the admin password after the first sign-in.
* [ ] HTTPS at a reverse proxy (Nginx, a load balancer) forwarding `X-Forwarded-*`; `SESSION_COOKIE_SECURE=true`.
* [ ] Replace the simulated payment in `OrderServiceImpl#pay` with a real gateway before taking real money.
* [ ] Replace `LoggingPasswordResetNotifier` with an e-mail sender (for example `spring-boot-starter-mail`).
* [ ] Regular MySQL backups (`mysqldump`).
* [ ] Several app instances behind a load balancer need shared sessions (sticky sessions or Spring Session).
