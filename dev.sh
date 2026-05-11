#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# dev.sh — single command to start all Community Bot services locally
#
# Usage:
#   ./dev.sh              # start everything
#   ./dev.sh --with-clamav  # also start ClamAV (slow first boot, ~300 MB)
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

for arg in "$@"; do
  [[ "$arg" == "--with-clamav" ]] && WITH_CLAMAV=true
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
  echo -e "  App         ${BLUE}http://localhost:3000${NC}"
  echo -e "  API         ${BLUE}http://localhost:8080${NC}"
  echo -e "  MinIO UI    ${BLUE}http://localhost:9001${NC}  (minioadmin / minioadmin)"
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
  command -v java   &>/dev/null || missing+=("java (JDK 21)")
  command -v docker &>/dev/null || missing+=("docker")
  command -v node   &>/dev/null || missing+=("node (v20+)")
  if [[ ${#missing[@]} -gt 0 ]]; then
    err "Missing required tools: ${missing[*]}"
    err "See RUNBOOK.md — Prerequisites for install instructions."
    exit 1
  fi
}

# ── Gradle wrapper ────────────────────────────────────────────────────────────
# gradlew exists (committed) but gradle-wrapper.jar is a binary that must be
# generated once. We use the system 'gradle' CLI (installed via Homebrew) to do
# this; if it's absent we install it automatically.
ensure_gradle_wrapper() {
  local jar="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar"
  [[ -f "$jar" ]] && return 0   # already present, nothing to do

  warn "gradle-wrapper.jar not found — generating Gradle wrapper (one-time setup)..."

  if ! command -v gradle &>/dev/null; then
    log "Installing Gradle via Homebrew (requires brew)..."
    if ! command -v brew &>/dev/null; then
      err "Homebrew not found. Install it from https://brew.sh, then re-run ./dev.sh"
      exit 1
    fi
    brew install gradle
  fi

  (cd "$SCRIPT_DIR" && gradle wrapper --gradle-version 9.1.0)
  chmod +x "$SCRIPT_DIR/gradlew"
  log "Gradle wrapper generated."
}

# ── .env ─────────────────────────────────────────────────────────────────────
load_env() {
  if [[ ! -f "$ENV_FILE" ]]; then
    err ".env not found at infra/.env"
    err "Run: cp infra/.env.example infra/.env  and fill in your secrets."
    exit 1
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
    if nc -z localhost "$port" 2>/dev/null; then
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
ensure_gradle_wrapper
mkdir -p "$LOG_DIR"

# ── 1. Infrastructure ─────────────────────────────────────────────────────────
log "Starting infrastructure containers..."
COMPOSE_SERVICES="postgres redis minio"
if [[ "$WITH_CLAMAV" == true ]]; then
  COMPOSE_SERVICES="$COMPOSE_SERVICES clamav"
  warn "ClamAV included. First start downloads ~300 MB of signatures (3-5 min)."
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
