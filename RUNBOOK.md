# Community Bot — Local Development Runbook

## Prerequisites

Install these tools once before your first run.

You do not need to install Gradle separately. The repository includes the Gradle wrapper, and all commands below use `./gradlew`.

| Tool | Min version | Install (macOS) |
|------|-------------|-----------------|
| Java JDK | **25** | [sdkman.io](https://sdkman.io) — `sdk install java 25.0.2-ms` (recommended) or [adoptium.net](https://adoptium.net) |
| Docker Desktop | latest | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop) |
| Node.js | 20 LTS | `brew install node` |

> **Java version matters.** The project targets JDK 25 and uses Lombok 1.18.46 which requires JDK 25+. Using an older JDK will produce a `NoSuchFieldException: TypeTag :: UNKNOWN` error at compile time.

Verify your setup:

```bash
java -version        # should print openjdk 25...
docker --version
node --version       # should print v20...
```

---

## Port map

| Port | Service | Notes |
|------|---------|-------|
| `3000` | React frontend | Vite dev server with hot-reload |
| `8080` | Spring Boot API | REST + WebSocket (`/ws`) |
| `5432` | PostgreSQL 16 | pgvector extension enabled |
| `6379` | Redis 7 | Pub/Sub, rate-limit cache, Streams |
| `9000` | MinIO S3 API | File/attachment storage |
| `9001` | MinIO web console | Bucket management UI |
| `3310` | ClamAV | Virus scanning (optional in dev) |

---

## Step 1 — Create your `.env` file

```bash
cd /path/to/capstone/infra
cp .env.example .env
```

Open `infra/.env` and fill in the required values as described below.

---

### 1a — Generate a JWT secret

Run this once and paste the output into `infra/.env`:

```bash
openssl rand -base64 64
```

```dotenv
JWT_SECRET=<paste output here>
```

---

### 1b — Register a Google OAuth app

1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Click the project dropdown at the top → **New Project** → name it (e.g. `community-bot-dev`) → **Create**
3. Left sidebar → **APIs & Services** → **OAuth consent screen**
   - User Type: **External** → **Create**
   - Fill in **App name** (e.g. `Community Bot`) and your **User support email**
   - Add your email under **Developer contact information**
   - Click **Save and Continue** through Scopes and Test Users (defaults are fine for local dev)
4. Left sidebar → **APIs & Services** → **Credentials**
   - **+ Create Credentials** → **OAuth 2.0 Client ID**
   - Application type: **Web application**
   - Name: `community-bot-local`
   - Under **Authorised redirect URIs** → **+ Add URI**:
     ```
     http://localhost:8080/login/oauth2/code/google
     ```
   - Click **Create**
5. Copy the **Client ID** and **Client Secret** from the popup into `infra/.env`:

```dotenv
GOOGLE_CLIENT_ID=123456789-abcdefg.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxx
```

---

### 1c — Register a GitHub OAuth app

1. Go to [github.com/settings/developers](https://github.com/settings/developers)
2. Click **New OAuth App**
3. Fill in:
   - **Application name**: `community-bot-local`
   - **Homepage URL**: `http://localhost:3000`
   - **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`
4. Click **Register application**
5. On the next page click **Generate a new client secret**
6. Copy both values into `infra/.env`:

```dotenv
GITHUB_CLIENT_ID=Ov23liXXXXXXXX
GITHUB_CLIENT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

> **Can I skip OAuth for now?** Yes. If you only want the REST APIs to work and don't need social login, leave the OAuth values blank. The dev profile uses placeholder values so the backend will still start — but clicking "Sign in with Google/GitHub" will fail. Fill in real credentials when you're ready to test the login flow.

---

### 1d — Set an OpenAI API key

Required for the RAG chat and AI moderation features. Get a key at [platform.openai.com/api-keys](https://platform.openai.com/api-keys).

```dotenv
OPENAI_API_KEY=sk-proj-...
```

> If you don't have a key, leave it blank. The backend starts fine without it — RAG queries and AI moderation will simply fail with a 500 when called. Set `MODERATION_ENABLED=false` (already the dev default) to suppress noisy errors.

---

### 1e — Multi-agent Ask Bot (optional)

The **Ask Bot** sidebar panel streams responses over **SSE** (`POST /api/workspaces/{id}/agent/ask/stream`) using a LangChain4j tool-calling assistant. It uses **Redis** for short conversation memory, **pgvector** tables for long-term memory and message semantic search, and respects per-workspace rate limits (`app.agent.daily-workspace-limit` in `application.yml`).

| Variable | Purpose |
|----------|---------|
| `TAVILY_API_KEY` | Enables **web search** tools for workspace **admins** when non-empty (`app.agent.web-search-admin-only` defaults to `true`). |

```dotenv
# Optional — leave empty to disable web search
TAVILY_API_KEY=
```

**Observability:** with management exposure configured, `GET /actuator/agents` reports agent invocation metrics (actuator is restricted when authenticated).

---

Everything else (`DB_URL`, `REDIS_*`, `MINIO_*`) matches the Docker Compose defaults and works without changes.

---

## Step 2 — Start infrastructure

```bash
cd /path/to/capstone/infra

# Start Postgres, Redis, and MinIO
docker compose up -d postgres redis minio

# Verify all three are healthy before proceeding
docker compose ps
```

Wait until the `STATUS` column shows `healthy` for all three containers (usually 10–20 seconds).

### ClamAV (optional)

ClamAV is **disabled by default** in the dev profile so you don't need it for normal development. Only start it when you want to test the file-upload virus-scanning pipeline.

```bash
docker compose up -d clamav
docker compose logs -f clamav   # wait for "Self-checking every 3600 seconds"
```

> **Apple Silicon note** — the ClamAV image has no native `arm64` build. The `docker-compose.yml` already pins it to `platform: linux/amd64`; Docker uses Rosetta 2 emulation transparently.

The first start downloads ~300 MB of virus signatures and can take **3–5 minutes**. Subsequent starts are fast.

---

## Step 3 — Create the MinIO bucket (first time only)

Open the MinIO web console: [http://localhost:9001](http://localhost:9001)

- Login: `minioadmin` / `minioadmin`
- Click **Create Bucket** → name it `community-bot` → Save

Or use the CLI if you have `mc` installed:

```bash
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/community-bot
```

---

## Step 4 — Start the backend

Open a new terminal tab:

```bash
cd /path/to/capstone

# Load .env values into the current shell
export $(grep -v '^#' infra/.env | xargs)

# Run Spring Boot with the dev profile
./gradlew :backend:bootRun --args='--spring.profiles.active=dev'
```

**What happens on first boot:**
- Flyway runs all database migrations through the latest version (including agent memory, message embeddings, and proposed scheduling actions) automatically.
- The bot system user (`bot@community-bot.internal`) is created by `BotUserInitializer`.

The backend is ready when you see:

```
Started CommunityBotApplication in X.XXX seconds (JVM running for Y.YYY)
```

It listens on `http://localhost:8080`.

### Dev profile behaviour

The `application-dev.yml` profile applies these overrides automatically:

| Feature | Dev default | How to enable |
|---------|-------------|---------------|
| ClamAV virus scanning | **disabled** | `export CLAMAV_ENABLED=true` before `bootRun` |
| AI content moderation | **disabled** | `export MODERATION_ENABLED=true` before `bootRun` |
| SQL logging | **on** | already on, set `show-sql: false` to silence it |
| JWT secret | hard-coded dev value | override with `JWT_SECRET` env var for any security testing |

---

## Step 5 — Start the frontend

Open another terminal tab:

```bash
cd /path/to/capstone/frontend
npm install        # first time only — installs node_modules
npm run dev
```

The frontend is ready when you see:

```
  VITE v5.x.x  ready in XXX ms

  ➜  Local:   http://localhost:3000/
```

---

## Step 6 — Open the app

Go to [http://localhost:3000](http://localhost:3000).

You can sign in with Google, GitHub, or the local demo accounts seeded in `dev`:

- `admin@communitybot.local` / `Admin123!`
- `mod@communitybot.local` / `Mod123!`
- `user@communitybot.local` / `User123!`

The OAuth flow redirects to the backend and back, sets an `HttpOnly` refresh-token cookie, and lands you on the dashboard.

---

## Everyday workflow

Once the initial setup is done, a single command starts everything:

```bash
./dev.sh
```

The script will:
1. Start Postgres, Redis, and MinIO (if not already running)
2. Create the MinIO bucket if it doesn't exist
3. Wait for each service to become healthy
4. Start the Spring Boot backend (logged to `.dev-logs/backend.log`)
5. Start the Vite frontend (logged to `.dev-logs/frontend.log`)
6. Open `http://localhost:3000` in your browser automatically

Press **Ctrl+C** to stop the backend and frontend. The Docker containers keep running.

To also start ClamAV for file upload testing:

```bash
./dev.sh --with-clamav
```

---

## Useful commands

### Docker

```bash
# Check container status
docker compose ps

# Tail logs for a specific service
docker compose logs -f postgres
docker compose logs -f clamav

# Stop all containers (data is preserved in volumes)
docker compose down

# Stop and wipe all data (full reset)
docker compose down -v
```

### Backend

```bash
# Run unit tests
./gradlew :backend:test

# Build a production JAR
./gradlew :backend:bootJar
# Output: backend/build/libs/backend-0.0.1-SNAPSHOT.jar

# Check the health endpoint
curl http://localhost:8080/actuator/health

# Apply only Flyway migrations without starting the server
./gradlew :backend:flywayMigrate
```

### Frontend

```bash
# Type-check without running the dev server
npm run build

# Lint
npm run lint
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `NoSuchFieldException: TypeTag :: UNKNOWN` (compile error) | JDK version too old — Lombok 1.18.46 requires JDK 25+ | Install JDK 25 (`sdk install java 25.0.2-ms`) and set it as default |
| `Client id of registration 'google' must not be empty` (startup crash) | OAuth env vars are blank or not exported | Either fill in real credentials (Steps 1b–1c) or just start the server — the dev profile already supplies placeholders so the server starts regardless. If you still see this, make sure you ran `export $(grep -v '^#' infra/.env \| xargs)` |
| `Environment variable JWT_SECRET must not be null` | `.env` not exported | Run `export $(grep -v '^#' infra/.env \| xargs)` before `bootRun` |
| `Connection refused localhost:5432` | Postgres not ready | `docker compose ps` — wait for `healthy`; if stuck, `docker compose restart postgres` |
| `Bucket 'community-bot' does not exist` | MinIO bucket not created | Open [http://localhost:9001](http://localhost:9001) and create the bucket (Step 3) |
| OAuth redirects back to `/login` with an error | Wrong callback URL registered | Confirm the redirect URI in your OAuth app matches exactly `http://localhost:8080/login/oauth2/code/google` (or `/github`) — no trailing slash |
| `no matching manifest for linux/arm64/v8` (ClamAV) | Missing arm64 image | The `docker-compose.yml` already has `platform: linux/amd64`; make sure you pulled the latest compose file |
| ClamAV stays `unhealthy` for minutes | DB download in progress | Wait 3–5 min on first start; run `docker compose logs -f clamav` to watch progress |
| OpenAI calls return 401 | Invalid or missing API key | Set `OPENAI_API_KEY` in `infra/.env`, re-export, restart the backend |
| Frontend shows a blank white screen | Backend CORS mismatch | Confirm `FRONTEND_URL=http://localhost:3000` is in `.env` and was exported |
