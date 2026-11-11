# Auth API (`/api/v1/auth`)

All bodies are JSON and every response uses the standard envelope (`success`, `data` or `error`, `timestamp`).
Interactive docs: Swagger UI at `/swagger-ui.html`, tag **1. Auth**.

| Method | Path | Auth | Purpose | Success |
|--------|------|------|---------|---------|
| POST | `/register` | public, rate limited | Create a customer account and sign it in | 201 + tokens |
| POST | `/login` | public, rate limited | Sign in with username or e-mail | 200 + tokens |
| POST | `/refresh` | public, rate limited | New token pair for a refresh token (rotation) | 200 + tokens |
| POST | `/logout` | public | Revoke this device's refresh token | 200 |
| POST | `/logout-all` | bearer | End every session on every device | 200 |
| GET | `/me` | bearer | The signed-in user | 200 |
| PUT | `/password` | bearer | Change password; other sessions end, this one gets new tokens | 200 + tokens |
| POST | `/forgot-password` | public, rate limited | E-mail a reset link (same answer for unknown addresses) | 202 |
| POST | `/reset-password` | public, rate limited | Set a new password with the token from the link | 200 |

## Token rules for clients

* Send `Authorization: Bearer <accessToken>`. Access tokens live **15 minutes**.
* When a call fails with `401` and `error.code = TOKEN_EXPIRED`, call `/refresh` once, store **both** new
  tokens, and retry the call.
* Refresh tokens live 30 days and are **single use**. Using one twice (for example two parallel refreshes)
  ends the whole sign-in with `REFRESH_TOKEN_REUSED`, so make sure only one refresh runs at a time.
* `INVALID_TOKEN` or `REFRESH_TOKEN_REUSED`: discard the tokens and show the sign-in screen.
* Store the refresh token in secure storage (iOS Keychain / Android Keystore, e.g. `flutter_secure_storage`).

## Examples

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json

{ "username": "dara", "email": "dara@mail.com", "password": "Secret@123", "fullName": "Sok Dara", "phone": "+855 12 345 678" }
```

```json
HTTP/1.1 201 Created
Location: /api/v1/auth/me

{
  "success": true,
  "message": "Account created",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "accessTokenExpiresAt": "2026-11-11T07:31:00Z",
    "refreshToken": "q3Jx0w8n6E1kP...",
    "refreshTokenExpiresAt": "2026-12-11T07:16:00Z",
    "user": { "id": 7, "username": "dara", "email": "dara@mail.com", "fullName": "Sok Dara",
              "phone": "+855 12 345 678", "enabled": true, "roles": ["ROLE_CUSTOMER"], ... }
  },
  "timestamp": "2026-11-11T07:16:00Z"
}
```

Password rule: 8-100 characters with an uppercase letter, a lowercase letter, a digit and a symbol.

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "login": "dara", "password": "Secret@123" }
```

Wrong password or unknown user (same answer, so accounts cannot be discovered):

```json
HTTP/1.1 401 Unauthorized

{ "success": false,
  "error": { "code": "INVALID_CREDENTIALS", "message": "Invalid username/email or password",
             "path": "/api/v1/auth/login", "traceId": "3ed44f28" },
  "timestamp": "2026-11-11T07:16:00Z" }
```

### Refresh

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{ "refreshToken": "q3Jx0w8n6E1kP..." }
```

Returns a new pair like login. The token in the request is now used up.

### Validation error

```json
HTTP/1.1 400 Bad Request

{ "success": false,
  "error": { "code": "VALIDATION_FAILED", "message": "Validation failed",
             "details": [ { "field": "email", "message": "must be a well-formed email address" },
                          { "field": "password", "message": "Password must be 8-100 characters and ..." } ],
             "path": "/api/v1/auth/register", "traceId": "9a1c02bd" },
  "timestamp": "..." }
```

### Rate limited

```
HTTP/1.1 429 Too Many Requests
Retry-After: 42

{ "success": false, "error": { "code": "RATE_LIMITED", "message": "Too many attempts. Please wait and try again", ... } }
```

Limits per client IP and minute: login 10, register 5, refresh 30, forgot-password 3, reset-password 5
(`app.rate-limit.rules`).

## curl

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"Admin@123"}' | jq -r .data.accessToken)
curl -s localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN" | jq
```
