#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR"

ENV_FILE="${ENV_FILE:-.env.deploy}"
if [ ! -f "$ENV_FILE" ]; then
  echo "[ERROR] missing $ENV_FILE; copy .env.deploy.example and fill it first" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

SERVICE_NAME="${SERVICE_NAME:-oms-java-platform}"
GIT_BRANCH="${GIT_BRANCH:-main}"
PORT="${PORT:-18020}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${PORT}/health}"
PROCESS_PATTERN="${PROCESS_PATTERN:-oms-java-platform}"
PID_FILE="${PID_FILE:-$APP_DIR/.pid}"
LOG_DIR="${LOG_DIR:-$APP_DIR/log}"
LOG_FILE="$LOG_DIR/${SERVICE_NAME}.log"
MAVEN_ARGS="${MAVEN_ARGS:--DskipTests -B}"

mkdir -p "$LOG_DIR"

log() {
  printf '[%s] [%s] %s\n' "$(date '+%F %T')" "$SERVICE_NAME" "$*"
}

pull_code() {
  log "syncing origin/$GIT_BRANCH"
  git fetch origin "$GIT_BRANCH"
  git checkout "$GIT_BRANCH"
  git pull --ff-only origin "$GIT_BRANCH"
  log "current commit $(git rev-parse --short HEAD)"
}

build_app() {
  local settings_args=()
  if [ -n "${MAVEN_SETTINGS:-}" ]; then
    settings_args=(-s "$MAVEN_SETTINGS")
  elif [ -f "$APP_DIR/maven-central-settings.xml" ]; then
    settings_args=(-s "$APP_DIR/maven-central-settings.xml")
  fi
  log "building jar"
  # shellcheck disable=SC2086
  mvn "${settings_args[@]}" clean package $MAVEN_ARGS
}

find_jar() {
  find "$APP_DIR/target" -maxdepth 1 -type f -name '*.jar' \
    ! -name '*sources.jar' ! -name '*javadoc.jar' ! -name 'original-*' \
    | sort | tail -n 1
}

stop_service() {
  local pids=""
  if [ -f "$PID_FILE" ]; then
    local pid
    pid="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      pids="$pid"
    fi
  fi
  local matched
  matched="$(pgrep -f "$PROCESS_PATTERN" 2>/dev/null || true)"
  if [ -n "$matched" ]; then
    pids="$(printf '%s\n%s\n' "$pids" "$matched" | awk 'NF && !seen[$0]++')"
  fi
  if [ -n "$pids" ]; then
    log "stopping pids: $(echo "$pids" | tr '\n' ' ')"
    echo "$pids" | xargs kill 2>/dev/null || true
    sleep 3
    echo "$pids" | while read -r pid; do
      [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null || true
    done
  fi
  rm -f "$PID_FILE"
}

start_service() {
  local jar
  jar="$(find_jar)"
  if [ -z "$jar" ]; then
    echo "[ERROR] built jar not found under target/" >&2
    exit 1
  fi
  log "starting $jar on port $PORT"
  # shellcheck disable=SC2086
  nohup java $JAVA_OPTS -Dservice.name="$SERVICE_NAME" -jar "$jar" >> "$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
  log "pid $(cat "$PID_FILE")"
}

wait_health() {
  local timeout="${HEALTH_TIMEOUT:-120}"
  local elapsed=0
  while [ "$elapsed" -lt "$timeout" ]; do
    if curl -fsS --max-time 3 "$HEALTH_URL" >/dev/null 2>&1; then
      log "healthy: $HEALTH_URL"
      return 0
    fi
    sleep 3
    elapsed=$((elapsed + 3))
  done
  log "[ERROR] health check timeout: $HEALTH_URL"
  tail -n 120 "$LOG_FILE" || true
  return 1
}

status_service() {
  if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    log "running pid $(cat "$PID_FILE")"
  else
    log "pid file missing or process stopped"
  fi
  curl -fsS --max-time 5 "$HEALTH_URL" || true
  echo
}

case "${1:-deploy}" in
  deploy)
    pull_code
    build_app
    stop_service
    start_service
    wait_health
    ;;
  build)
    build_app
    ;;
  start)
    start_service
    wait_health
    ;;
  stop)
    stop_service
    ;;
  restart)
    stop_service
    start_service
    wait_health
    ;;
  status)
    status_service
    ;;
  *)
    echo "Usage: $0 {deploy|build|start|stop|restart|status}" >&2
    exit 2
    ;;
esac
