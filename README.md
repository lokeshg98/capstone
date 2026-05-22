# Community Bot

Community Bot is a Slack/Discord-style workspace app with chat, bots, moderation, scheduled posts, and file uploads.

## Quickstart

If you already installed the tools, do this:

1. Copy the example settings file.

   Windows PowerShell:

   ```powershell
   Copy-Item infra/.env.example infra/.env
   ```

   Linux:

   ```bash
   cp infra/.env.example infra/.env
   ```

2. Start the database and support services.

   ```bash
   docker compose -f infra/docker-compose.yml up -d postgres redis minio
   ```

3. Start the backend.

   Windows PowerShell:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "dev"
   .\gradlew.bat :backend:bootRun
   ```

   Linux:

   ```bash
   export SPRING_PROFILES_ACTIVE=dev
   ./gradlew :backend:bootRun
   ```

4. Start the frontend.

   Windows PowerShell:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

   Linux:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. Open the app in your browser.

   Go to `http://localhost:3000`.

## Before You Start

Make sure these are installed:

- Git
- Java JDK 25
- Node.js 20 LTS
- Docker Desktop on Windows, or Docker Engine plus Docker Compose on Linux

If you are new to computers, a terminal is a window where you type commands.

## Setup

Follow the steps below in order.

### 1. Download the tools

Windows:
- Install Git: https://git-scm.com/download/win
- Install Java JDK 25: https://adoptium.net/
- Install Node.js 20 LTS: https://nodejs.org/
- Install Docker Desktop: https://www.docker.com/products/docker-desktop/

Linux:
- Install Git with your package manager
- Install Java JDK 25: https://adoptium.net/
- Install Node.js 20 LTS: https://nodejs.org/
- Install Docker Engine and Docker Compose, or Docker Desktop for Linux

### 2. Open a terminal

Windows:
- Open PowerShell or Windows Terminal

Linux:
- Open your terminal app

### 3. Get the project files

If you have not downloaded the project yet, clone it.

```bash
git clone <repo-url>
cd capstone
```

Replace `<repo-url>` with the real repository address.

### 4. Create the environment file

The app reads settings from `infra/.env`.

Windows PowerShell:

```powershell
Copy-Item infra/.env.example infra/.env
```

Linux:

```bash
cp infra/.env.example infra/.env
```

Open `infra/.env` in a text editor and fill in these values:

- `JWT_SECRET` - a long random string
- `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` - needed for Google sign-in
- `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` - needed for GitHub sign-in
- `OPENAI_API_KEY` - needed for bot Q&A and moderation

If you only want to check that the app starts, you can leave the OAuth and OpenAI values blank for now.

To make a JWT secret, use one of these commands.

Windows PowerShell:

```powershell
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Linux:

```bash
openssl rand -base64 64
```

### 5. Start the local services

From the project root, start PostgreSQL, Redis, and MinIO.

```bash
docker compose -f infra/docker-compose.yml up -d postgres redis minio
```

Wait a few seconds, then check that they are running.

```bash
docker compose -f infra/docker-compose.yml ps
```

### 6. Create the MinIO bucket

Open this page in your browser:

- http://localhost:9001

Log in with:

- Username: `minioadmin`
- Password: `minioadmin`

Create a bucket named:

- `community-bot`

### 7. Start the backend

Open a second terminal window in the project root.

Windows:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\gradlew.bat :backend:bootRun
```

Linux:

```bash
export SPRING_PROFILES_ACTIVE=dev
./gradlew :backend:bootRun
```

The backend should start at:

- http://localhost:8080

### 8. Start the frontend

Open a third terminal window in the `frontend` folder.

Windows:

```powershell
cd frontend
npm install
npm run dev
```

Linux:

```bash
cd frontend
npm install
npm run dev
```

The frontend should start at:

- http://localhost:3000

### 9. Open the app

Go to:

- http://localhost:3000

Then sign in with Google or GitHub if you set up those credentials.

## Daily Start

If you are on Linux, you can usually start everything with:

```bash
./dev.sh
```

On Windows, start the services, backend, and frontend in separate terminals.

## Useful Checks

- Backend health: `http://localhost:8080/actuator/health`
- Frontend build check: `cd frontend && npm run build`
- Backend tests: `./gradlew :backend:test`

## Troubleshooting

- If login does not work, check the OAuth values in `infra/.env`.
- If bot features do not work, check `OPENAI_API_KEY`.
- If file uploads fail, make sure MinIO is running and the `community-bot` bucket exists.
