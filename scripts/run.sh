#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:${DB_PORT:-5434}/techstore}"
export DB_USER="${DB_USER:-techstore}"
export DB_PASSWORD="${DB_PASSWORD:-techstore_local}"
export PORT="${PORT:-8081}"
docker compose up -d --wait
exec ./mvnw --batch-mode --no-transfer-progress spring-boot:run
