# LERA Backend — Java Spring Boot

> Location-based Emergency Response App  
> Semicolon Capstone Project | Spring Boot 3.2 · PostgreSQL · Socket.IO · JWT + Refresh Tokens · Firebase FCM

---

## Architecture Overview

```
Event-Driven Architecture (EDA)
────────────────────────────────────────────────────────────────
HTTP Request → Controller → Service → ApplicationEventPublisher
                                              │
                                    EmergencyEventListener
                                    (async, non-blocking)
                                    ├── Socket.IO broadcast
                                    └── FCM push + DB notification
```

Every emergency lifecycle change (created, accepted, declined, resolved, cancelled)
publishes a Spring `ApplicationEvent`. The listener handles Socket.IO and FCM
notifications asynchronously, so the HTTP response is never blocked.

---

## Project Structure

```
src/main/java/com/lera/
├── LeraApplication.java            Entry point
├── config/
│   ├── AppConfig.java              Enables @Scheduled tasks
│   ├── FirebaseConfig.java         Firebase Admin SDK init
│   ├── SecurityConfig.java         JWT filter chain + CORS
│   └── SocketIOConfig.java         Netty-SocketIO server config
├── controller/
│   ├── AuthController.java         /api/v1/auth/**
│   ├── EmergencyController.java    /api/v1/emergencies/**
│   ├── HealthController.java       /health
│   ├── NotificationController.java /api/v1/notifications/**
│   └── ResponderController.java    /api/v1/responders/**
├── dto/
│   ├── request/                    Validated inbound payloads
│   └── response/                   Outbound JSON shapes
├── event/                          Spring ApplicationEvents (EDA)
│   ├── EmergencyCreatedEvent.java
│   ├── EmergencyAcceptedEvent.java
│   ├── EmergencyDeclinedEvent.java
│   ├── EmergencyResolvedEvent.java
│   └── EmergencyCancelledEvent.java
├── exception/
│   ├── AppException.java           Custom HTTP exception
│   └── GlobalExceptionHandler.java @RestControllerAdvice
├── model/                          JPA entities + enums
├── repository/                     Spring Data JPA repositories
├── security/
│   ├── JwtAuthFilter.java          Reads Bearer token per request
│   └── JwtService.java             Sign / validate access tokens
├── service/
│   ├── AuthService.java            Register, login, refresh, logout
│   ├── EmergencyEventListener.java @Async EDA listener
│   ├── EmergencyService.java       Emergency lifecycle logic
│   ├── NotificationService.java    DB notifications + FCM
│   ├── RefreshTokenService.java    Issue / rotate / revoke
│   ├── ResponderService.java       Profile, availability, location
│   └── UserDetailsServiceImpl.java Spring Security bridge
└── socket/
    └── SocketIOService.java        Netty-SocketIO emit helpers
```

---

## API Reference

All endpoints are prefixed `/api/v1`. Protected routes require:
```
Authorization: Bearer <access_token>
```

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | ❌ | Register citizen or responder |
| POST | `/auth/login` | ❌ | Login → access + refresh token |
| POST | `/auth/refresh` | ❌ | Exchange refresh token for new access token |
| POST | `/auth/logout` | ✅ | Revoke refresh token(s) |
| GET  | `/auth/me` | ✅ | Get current user profile |
| PATCH | `/auth/me` | ✅ | Update fullName / phoneNumber |
| PATCH | `/auth/me/fcm-token` | ✅ | Update FCM push token |

**Register body:**
```json
{
  "fullName": "Ada Okonkwo",
  "email": "ada@example.com",
  "phoneNumber": "+2348012345678",
  "password": "secret123",
  "role": "citizen"
}
```
Responder extra fields: `"role": "responder"`, `"certificationId": "NPF-001"`, `"responderType": "police"`

**Login response:**
```json
{
  "status": "success",
  "data": {
    "token": "<access_jwt>",
    "refreshToken": "<opaque_token>",
    "expiresIn": 900,
    "user": { "id": "...", "fullName": "...", "role": "citizen", ... }
  }
}
```

**Refresh body:**
```json
{ "refreshToken": "<opaque_token>" }
```

### Emergencies

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/emergencies` | citizen | Report emergency |
| GET  | `/emergencies/active` | any | Get active emergency |
| GET  | `/emergencies/history` | any | Get past emergencies |
| GET  | `/emergencies/:id` | any | Get by ID |
| PATCH | `/emergencies/:id/accept` | responder | Accept |
| PATCH | `/emergencies/:id/decline` | responder | Decline |
| PATCH | `/emergencies/:id/resolve` | responder | Mark resolved |
| PATCH | `/emergencies/:id/cancel` | citizen | Cancel |

**Create body:**
```json
{
  "type": "medical",
  "incidentLat": 6.5244,
  "incidentLng": 3.3792,
  "district": "Victoria Island"
}
```
Types: `injury` | `fire` | `medical` | `other`

### Responders

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/responders/profile` | responder | Get own profile |
| PATCH | `/responders/availability` | responder | Set online/offline |
| PATCH | `/responders/location` | responder | Update GPS |

### Notifications

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/notifications` | ✅ | List all |
| PATCH | `/notifications/read-all` | ✅ | Mark all read |
| PATCH | `/notifications/:id/read` | ✅ | Mark one read |

---

## Token Strategy

| Token | Type | TTL | Storage |
|-------|------|-----|---------|
| Access token | Signed JWT | 15 minutes | Memory / `localStorage` |
| Refresh token | Opaque (64-byte random) | **7–30 days** | `localStorage` (send on refresh call) |

**Rotation:** Every `/auth/refresh` call invalidates the old refresh token and
issues a new one. If a stolen token is reused, all tokens for that user are revoked.

**Frontend usage:**
```
1. Login → store token + refreshToken
2. Every API call → Authorization: Bearer <token>
3. On 401 → call POST /auth/refresh with { refreshToken }
4. Store new token + refreshToken, retry original request
5. Logout → call POST /auth/logout
```

To change refresh token TTL, set env var:
```
REFRESH_TOKEN_EXPIRATION_MS=1209600000   # 14 days
REFRESH_TOKEN_EXPIRATION_MS=2592000000   # 30 days
```

---

## Real-time Socket.IO Events

The Socket.IO server runs on port `9092` (separate from HTTP on `8080`).

**Frontend connection:**
```js
const socket = io("https://your-backend.onrender.com", {
  auth: { token: "<jwt_access_token>" },
  transports: ["websocket", "polling"]
});
```

**Events emitted by server:**

| Event | Payload | When |
|-------|---------|------|
| `emergency:new` | `EmergencyDto` | New emergency created — sent to ALL clients |
| `emergency:updated` | `EmergencyDto` | Status changed — sent to citizen + responder |
| `responder:location` | `{ responderId, lat, lng }` | Responder location update |

---

## Dependencies to Install

You need **Java 17** and **Maven 3.8+** installed on your machine.

### Check what you have

```bash
java -version     # Need: 17+
mvn -version      # Need: 3.8+
```

### Install Java 17

**Ubuntu / Debian (WSL included):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
```

**macOS (Homebrew):**
```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Windows:**
Download from https://adoptium.net → Eclipse Temurin 17 → run `.msi` installer

### Install Maven

**Ubuntu / Debian:**
```bash
sudo apt install maven -y
```

**macOS:**
```bash
brew install maven
```

**Windows:**
Download from https://maven.apache.org/download.cgi → unzip → add `bin/` to PATH

### Maven auto-downloads all Java dependencies

When you run `mvn clean package`, Maven downloads everything automatically from
Maven Central. No manual jar downloads needed. The `pom.xml` declares:

| Dependency | Version | Purpose |
|------------|---------|---------|
| spring-boot-starter-web | 3.2.5 | REST API, embedded Tomcat |
| spring-boot-starter-data-jpa | 3.2.5 | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.2.5 | JWT filter chain |
| spring-boot-starter-validation | 3.2.5 | @Valid request validation |
| postgresql | latest | PostgreSQL JDBC driver |
| jjwt-api/impl/jackson | 0.12.5 | JWT sign + verify |
| netty-socketio | 2.0.6 | Socket.IO server |
| firebase-admin | 9.2.0 | FCM push notifications |
| lombok | latest | Boilerplate reduction |
| h2 (test) | latest | In-memory DB for tests |

---

## Local Development Setup

### 1. PostgreSQL (local)

```bash
# Ubuntu
sudo apt install postgresql -y
sudo -u postgres psql -c "CREATE USER lera_user WITH PASSWORD 'lera_pass';"
sudo -u postgres psql -c "CREATE DATABASE lera_db OWNER lera_user;"

# macOS
brew install postgresql@15
brew services start postgresql@15
psql postgres -c "CREATE USER lera_user WITH PASSWORD 'lera_pass';"
psql postgres -c "CREATE DATABASE lera_db OWNER lera_user;"
```

### 2. Environment variables

Create a `.env` file **or** export variables in your shell:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/lera_db
export DB_USERNAME=lera_user
export DB_PASSWORD=lera_pass
export JWT_SECRET=YourSuperSecretKeyThatIsAtLeast256BitsLongChangeThis!!
export JWT_ACCESS_EXPIRATION_MS=900000
export REFRESH_TOKEN_EXPIRATION_MS=604800000
export SOCKETIO_PORT=9092
export CORS_ORIGINS=http://localhost:3000,http://localhost:3001
# Optional — leave blank to disable FCM
export FIREBASE_SERVICE_ACCOUNT_JSON=
```

### 3. Build and run

```bash
cd lera-backend

# Download dependencies + compile + run tests
mvn clean package

# Start the server
java -jar target/lera-backend.jar
```

Or use the Maven wrapper (no mvn install needed):
```bash
./mvnw spring-boot:run
```

Server starts at:
- HTTP API → http://localhost:8080
- Socket.IO → http://localhost:9092
- Health check → http://localhost:8080/health

---

## Deploy to Render

### Step 1 — Push to GitHub

```bash
cd lera-backend
git init
git add .
git commit -m "feat: LERA Java backend"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/lera-backend.git
git push -u origin main
```

### Step 2 — Create Render Web Service

1. Go to https://render.com → **New → Web Service**
2. Connect your GitHub repo `lera-backend`
3. Configure:

| Field | Value |
|-------|-------|
| **Runtime** | Java |
| **Build Command** | `./mvnw clean package -DskipTests` |
| **Start Command** | `java -jar target/lera-backend.jar` |
| **Health Check Path** | `/health` |
| **Plan** | Free (or Starter for always-on) |

### Step 3 — Create PostgreSQL database

1. Render Dashboard → **New → PostgreSQL**
2. Name: `lera-db`
3. Click **Create Database**
4. Copy the **Internal Database URL**

### Step 4 — Set environment variables

In your Render web service → **Environment** tab, add:

| Key | Value |
|-----|-------|
| `DATABASE_URL` | (from Render PostgreSQL → Internal Connection String) |
| `JWT_SECRET` | Generate: `openssl rand -base64 64` |
| `JWT_ACCESS_EXPIRATION_MS` | `900000` |
| `REFRESH_TOKEN_EXPIRATION_MS` | `604800000` (7 days) |
| `SOCKETIO_PORT` | `9092` |
| `CORS_ORIGINS` | `https://your-lera-frontend.vercel.app` |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | *(optional — paste JSON for FCM)* |

> **Render DATABASE_URL format:** Render gives `postgresql://user:pass@host/db`.
> Spring needs `jdbc:postgresql://...`. Add this startup flag if needed:
> ```
> java -jar target/lera-backend.jar --spring.datasource.url=${DATABASE_URL}
> ```
> Or use the `JDBC_DATABASE_URL` env var that Render also exposes.

### Step 5 — Update frontend

In your Vercel frontend → Environment Variables:
```
NEXT_PUBLIC_API_URL=https://lera-backend.onrender.com/api/v1
```

### Step 6 — Deploy

Push any commit to `main` — Render auto-deploys.

```bash
git push origin main
```

---

## Update Frontend for Refresh Tokens

Add this to `src/lib/api.ts` to handle automatic token refresh on 401:

```typescript
// In your request() function, after getting a 401 response:
if (res.status === 401) {
  const refreshToken = localStorage.getItem("lera_refresh_token");
  if (refreshToken) {
    const refreshRes = await fetch(`${BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (refreshRes.ok) {
      const refreshData = await refreshRes.json();
      localStorage.setItem("lera_token", refreshData.data.token);
      localStorage.setItem("lera_refresh_token", refreshData.data.refreshToken);
      // Retry original request with new token
      headers["Authorization"] = `Bearer ${refreshData.data.token}`;
      return fetch(`${BASE_URL}${endpoint}`, { method, headers, body: ... });
    }
  }
  // Refresh failed — redirect to login
  localStorage.clear();
  window.location.href = "/auth/login";
}
```

Also update `authStore.ts` to store `refreshToken`:
```typescript
setAuth: (user, token, refreshToken) => {
  localStorage.setItem("lera_token", token);
  localStorage.setItem("lera_refresh_token", refreshToken);
  localStorage.setItem("lera_user", JSON.stringify(user));
  ...
}
```

---

## Firebase FCM Setup (Optional)

1. Go to https://console.firebase.google.com
2. Create project → **Project Settings → Service Accounts**
3. Click **Generate new private key** → download JSON
4. Minify the JSON to a single line:
   ```bash
   cat firebase-key.json | jq -c . 
   ```
5. Paste the single-line JSON into the `FIREBASE_SERVICE_ACCOUNT_JSON` env var on Render

If not configured, the app works fully — push notifications are simply skipped.

---

## Common Issues

**`Error: DATABASE_URL` format**
Render PostgreSQL URLs start with `postgres://`. Spring needs `jdbc:postgresql://`.
Fix: In Render env vars, manually set `DATABASE_URL` with `jdbc:postgresql://` prefix.

**Socket.IO port on Render free tier**
Render free tier only exposes port 8080 (your `PORT`). Socket.IO on 9092 won't be
externally accessible on free tier. Solutions:
- Upgrade to Starter plan (multiple ports), OR
- Run Socket.IO on the same port as HTTP (change `SOCKETIO_PORT=8080` and ensure
  no port collision — netty-socketio and Tomcat can't share a port natively)
- **Recommended free tier fix:** Use polling transport only, or upgrade plan

**`OutOfMemoryError` on Render free tier**
Add JVM flags to start command:
```
java -Xmx400m -Xms200m -jar target/lera-backend.jar
```

**Tests fail locally**
Tests use H2 in-memory database. Make sure no local PostgreSQL config conflicts.
Run with:
```bash
mvn test
```
