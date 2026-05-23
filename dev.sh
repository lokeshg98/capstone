#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# dev.sh — single command to start all Community Bot services locally
#
# Usage:
#   ./dev.sh                # start everything (DB, Redis, MinIO, backend, frontend)
#   ./dev.sh --with-clamav  # also start ClamAV (slow first boot, ~300 MB)
#   ./dev.sh --with-n8n     # also start n8n (weekly digest automation)
#
# Press Ctrl+C to stop the backend and frontend.
# Docker containers keep running; stop them with:
#   docker compose -f infra/docker-compose.yml down
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/infra/.env"
LOG_DIR="$SCRIPT_DIR/.dev-logs"
WITH_CLAMAV=false
WITH_N8N=false

for arg in "$@"; do
  [[ "$arg" == "--with-clamav" ]] && WITH_CLAMAV=true
  [[ "$arg" == "--with-n8n"    ]] && WITH_N8N=true
done

# ── Colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

log()  { echo -e "${GREEN}[dev]${NC} $*"; }
warn() { echo -e "${YELLOW}[dev]${NC} $*"; }
err()  { echo -e "${RED}[dev]${NC} $*" >&2; }
banner() {
  echo
  echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${GREEN}${BOLD}  Community Bot is running!${NC}"
  echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "  App         ${BLUE}http://localhost:3000${NC}  (React UI)"
  echo -e "  Home/API    ${BLUE}http://localhost:8080${NC}  (landing + OAuth + API)"
  echo -e "  MinIO UI    ${BLUE}http://localhost:9001${NC}  (minioadmin / minioadmin)"
  if [[ "$WITH_N8N" == true ]]; then
    echo -e "  n8n         ${BLUE}http://localhost:5678${NC}  (admin / admin)"
  fi
  echo -e ""
  echo -e "  Demo accounts:"
  echo -e "    ${YELLOW}admin@communitybot.local${NC}  /  Admin123!"
  echo -e "    ${YELLOW}mod@communitybot.local${NC}   /  Mod123!"
  echo -e "    ${YELLOW}user@communitybot.local${NC}   /  User123!"
  echo -e ""
  echo -e "  Backend log   .dev-logs/backend.log"
  echo -e "  Frontend log  .dev-logs/frontend.log"
  echo -e ""
  echo -e "  Press ${YELLOW}Ctrl+C${NC} to stop"
  echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo
}

# ── Prerequisites ─────────────────────────────────────────────────────────────
check_prereqs() {
  local missing=()
  command -v java   &>/dev/null || missing+=("java (JDK 25)")
  command -v docker &>/dev/null || missing+=("docker")
  command -v node   &>/dev/null || missing+=("node (v20+)")
  if [[ ${#missing[@]} -gt 0 ]]; then
    err "Missing required tools: ${missing[*]}"
    err "See RUNBOOK.md — Prerequisites for install instructions."
    exit 1
  fi
}

# ── Gradle wrapper ────────────────────────────────────────────────────────────
check_gradle_wrapper() {
  local jar="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar"
  if [[ -f "$jar" ]]; then
    return 0
  fi

  err "Missing gradle/wrapper/gradle-wrapper.jar."
  err "Re-clone the repo or restore the wrapper binary from git, then run ./dev.sh again."
  exit 1
}

# ── .env ─────────────────────────────────────────────────────────────────────
load_env() {
  if [[ ! -f "$ENV_FILE" ]]; then
    warn "infra/.env not found — creating from infra/.env.example"
    cp "$SCRIPT_DIR/infra/.env.example" "$ENV_FILE"

    if command -v openssl &>/dev/null; then
      local jwt
      jwt=$(openssl rand -base64 64)
      sed -i.bak "s|JWT_SECRET=REPLACE_WITH_STRONG_BASE64_SECRET|JWT_SECRET=$jwt|" "$ENV_FILE"
      rm -f "$ENV_FILE.bak"
      log "Generated a random JWT_SECRET."
    else
      warn "openssl not found — JWT_SECRET is still a placeholder."
    fi

    warn "infra/.env has been created from the template."
    warn "Edit it to add real OAuth & OpenAI keys for full functionality."
  fi

  local jwt_val
  jwt_val=$(grep '^JWT_SECRET=' "$ENV_FILE" | cut -d= -f2-)
  if [[ "$jwt_val" == "REPLACE_WITH_STRONG_BASE64_SECRET" && "$(command -v openssl)" ]]; then
    local new_jwt
    new_jwt=$(openssl rand -base64 64)
    sed -i.bak "s|JWT_SECRET=REPLACE_WITH_STRONG_BASE64_SECRET|JWT_SECRET=$new_jwt|" "$ENV_FILE"
    rm -f "$ENV_FILE.bak"
    log "Replaced placeholder JWT_SECRET with a random key."
  fi

  # Export every non-comment, non-empty line
  set -a
  # shellcheck source=/dev/null
  source "$ENV_FILE"
  set +a
  log "Loaded infra/.env"
}

# ── Wait helpers ──────────────────────────────────────────────────────────────
# wait_for <label> <test-command> [max-seconds]
wait_for() {
  local name="$1" cmd="$2" max="${3:-30}"
  printf "[dev] Waiting for %s" "$name"
  for _ in $(seq 1 "$max"); do
    if eval "$cmd" &>/dev/null; then
      echo -e " ${GREEN}ready${NC}"
      return 0
    fi
    printf "."
    sleep 1
  done
  echo -e " ${RED}timed out${NC}"
  err "$name did not become ready in ${max}s."
  err "Check logs: docker compose -f infra/docker-compose.yml logs $name"
  exit 1
}

# wait_for_port <label> <port> [max-seconds]
wait_for_port() {
  local name="$1" port="$2" max="${3:-30}"
  printf "[dev] Waiting for %s (port %s)" "$name" "$port"
  for _ in $(seq 1 "$max"); do
    if command -v curl &>/dev/null; then
      if curl -fsS "http://127.0.0.1:${port}/" >/dev/null 2>&1 || curl -fsS "http://localhost:${port}/" >/dev/null 2>&1; then
        echo -e " ${GREEN}ready${NC}"
        return 0
      fi
    elif nc -z 127.0.0.1 "$port" 2>/dev/null || nc -z localhost "$port" 2>/dev/null; then
      echo -e " ${GREEN}ready${NC}"
      return 0
    fi
    printf "."
    sleep 1
  done
  echo -e " ${RED}timed out${NC}"
  err "$name did not open port $port in ${max}s."
  exit 1
}

# ── Cleanup on Ctrl+C ─────────────────────────────────────────────────────────
BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  echo
  warn "Shutting down backend and frontend..."
  [[ -n "$BACKEND_PID"  ]] && kill "$BACKEND_PID"  2>/dev/null || true
  [[ -n "$FRONTEND_PID" ]] && kill "$FRONTEND_PID" 2>/dev/null || true
  log "Docker containers are still running."
  log "Stop them with: docker compose -f infra/docker-compose.yml down"
}
trap cleanup EXIT INT TERM

# ─────────────────────────────────────────────────────────────────────────────
# MAIN
# ─────────────────────────────────────────────────────────────────────────────

check_prereqs
load_env
check_gradle_wrapper
mkdir -p "$LOG_DIR"

# ── 1. Infrastructure ─────────────────────────────────────────────────────────
log "Starting infrastructure containers..."
COMPOSE_SERVICES="postgres redis minio"
if [[ "$WITH_CLAMAV" == true ]]; then
  COMPOSE_SERVICES="$COMPOSE_SERVICES clamav"
  warn "ClamAV included. First start downloads ~300 MB of signatures (3-5 min)."
fi
if [[ "$WITH_N8N" == true ]]; then
  COMPOSE_SERVICES="$COMPOSE_SERVICES n8n"
  warn "n8n included (http://localhost:5678 — admin / admin)."
fi

# shellcheck disable=SC2086
docker compose -f "$SCRIPT_DIR/infra/docker-compose.yml" up -d $COMPOSE_SERVICES

wait_for "postgres" "docker exec cb-postgres pg_isready -U communitybot" 30
wait_for "redis"    "docker exec cb-redis redis-cli ping"                 20
wait_for "minio"    "curl -sf http://localhost:9000/minio/health/live"    20

if [[ "$WITH_CLAMAV" == true ]]; then
  wait_for "clamav" "nc -z localhost 3310" 300
fi

# ── 2. MinIO bucket ───────────────────────────────────────────────────────────
log "Ensuring MinIO bucket 'community-bot' exists..."
docker exec cb-minio mc alias set local http://localhost:9000 minioadmin minioadmin &>/dev/null
docker exec cb-minio mc mb --ignore-existing local/community-bot &>/dev/null
log "Bucket ready."

# ── 3. Backend ────────────────────────────────────────────────────────────────
log "Starting backend  (logs → .dev-logs/backend.log)..."
(
  cd "$SCRIPT_DIR"
  ./gradlew :backend:bootRun --args='--spring.profiles.active=dev'
) > "$LOG_DIR/backend.log" 2>&1 &
BACKEND_PID=$!

printf "[dev] Waiting for backend (Spring Boot)"
for _ in $(seq 1 90); do
  if curl -sf http://localhost:8080/actuator/health &>/dev/null; then
    echo -e " ${GREEN}ready${NC}"
    break
  fi
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo -e " ${RED}crashed${NC}"
    err "Backend failed to start. Last 20 lines of .dev-logs/backend.log:"
    tail -20 "$LOG_DIR/backend.log" >&2
    exit 1
  fi
  printf "."
  sleep 2
done

# ── 4. Frontend ───────────────────────────────────────────────────────────────
log "Starting frontend (logs → .dev-logs/frontend.log)..."
(
  cd "$SCRIPT_DIR/frontend"
  npm install --prefer-offline --silent
  npm run dev
) > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!

wait_for_port "frontend (Vite)" 3000 30

# ── 5. Open browser ───────────────────────────────────────────────────────────
open http://localhost:3000 2>/dev/null || true

# ── Done ──────────────────────────────────────────────────────────────────────
banner

# Keep the script alive so Ctrl+C triggers cleanup
wait "$BACKEND_PID" "$FRONTEND_PID"
