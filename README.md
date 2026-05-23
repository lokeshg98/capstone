# Community Bot

A Slack/Discord-style workspace app with real-time chat, AI-powered bots, moderation, scheduled posts, file uploads, and more.

## Quickstart

```bash
./dev.sh
```

That's it. One command starts everything — PostgreSQL, Redis, MinIO, the backend, and the frontend. Once the banner appears, open **http://localhost:3000**.

**First run only** — `dev.sh` automatically creates `infra/.env` from the template and generates a random `JWT_SECRET` for you.

### Demo accounts (no OAuth setup needed)

| Email | Password | Role |
|---|---|---|
| `admin@communitybot.local` | `Admin123!` | Admin |
| `mod@communitybot.local` | `Mod123!` | Moderator |
| `user@communitybot.local` | `User123!` | Member |

The accounts, a demo organization, and a workspace are auto-seeded in dev mode.

### Extra services

```bash
./dev.sh --with-clamav   # virus scanning for file uploads (slow first boot)
./dev.sh --with-n8n      # workflow automation for weekly digests
```

Press `Ctrl+C` to stop the backend and frontend. To stop Docker containers:

```bash
docker compose -f infra/docker-compose.yml down
```

## Prerequisites

- **Java JDK 25** ([adoptium.net](https://adoptium.net))
- **Docker** (Engine + Compose on Linux, or Docker Desktop)
- **Node.js 20 LTS** ([nodejs.org](https://nodejs.org))

## Enabling full features

| Feature | What you need | Where to set it |
|---|---|---|
| Google / GitHub login | OAuth client ID + secret | `infra/.env` |
| Ask Bot (AI Q&A) | OpenAI API key | `infra/.env` |
| Virus scanning | ClamAV Docker container | `./dev.sh --with-clamav` |
| Weekly digests | n8n Docker container | `./dev.sh --with-n8n` |

Edit `infra/.env` to add your keys:
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`
- `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET`
- `OPENAI_API_KEY`

## Ports

| Service | URL |
|---|---|
| App (React) | http://localhost:3000 |
| API (Spring Boot) | http://localhost:8080 |
| MinIO console | http://localhost:9001 |
| n8n (optional) | http://localhost:5678 |

## Useful commands

```bash
./gradlew :backend:test          # run backend tests
cd frontend && npm run build     # verify frontend compiles
```

For detailed setup steps and troubleshooting, see [RUNBOOK.md](RUNBOOK.md).
