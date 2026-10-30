#!/usr/bin/env bash
# Usage: ./start-all.sh [--no-build]
set -uo pipefail
cd "$(dirname "$0")"

SERVICES=(auth-service:8081 catalog-service:8082 booking-service:8083 payment-service:8084
          notification-service:8085 seat-events-service:8086 api-gateway:8080)
mkdir -p logs .pids

running() { [ -f ".pids/$1" ] && kill -0 "$(cat ".pids/$1")" 2>/dev/null; }

echo "Starting Postgres, Redis, Kafka and dashboards..."
docker compose up -d || exit 1

if [ "${1:-}" != "--no-build" ]; then
  echo "Building services..."
  mvn -q package -DskipTests || { echo "Build failed. Fix the error above and run again."; exit 1; }
fi

sleep 3  # give Kafka and Postgres a moment on first start

for entry in "${SERVICES[@]}"; do
  s=${entry%%:*}
  if running "$s"; then echo "  $s is already running"; continue; fi
  jar=$(ls "$s"/target/*.jar 2>/dev/null | head -1)
  [ -z "$jar" ] && { echo "  $s has no jar. Run without --no-build."; exit 1; }
  nohup java -Xmx256m -jar "$jar" > "logs/$s.log" 2>&1 &
  echo $! > ".pids/$s"
  echo "  started $s"
done

if [ -d holdmyseat-web/node_modules ] && ! running web; then
  (cd holdmyseat-web && setsid nohup npm run dev > ../logs/web.log 2>&1 & echo $! > ../.pids/web)
  echo "  started web app"
elif [ -d holdmyseat-web ] && [ ! -d holdmyseat-web/node_modules ]; then
  echo "  skipped web app: run 'npm install' in holdmyseat-web first"
fi

echo
echo "Waiting for services to report healthy (up to 2 minutes)..."
deadline=$((SECONDS + 120))
for entry in "${SERVICES[@]}"; do
  s=${entry%%:*}; port=${entry##*:}
  until curl -sf "http://localhost:$port/actuator/health" > /dev/null; do
    if ! running "$s"; then echo "  ✘ $s crashed. See logs/$s.log"; continue 2; fi
    if [ $SECONDS -gt $deadline ]; then echo "  ✘ $s not healthy yet. See logs/$s.log"; continue 2; fi
    sleep 2
  done
  echo "  ✔ $s  http://localhost:$port"
done

echo
echo "App:          http://localhost:5173"
echo "Kafka UI:     http://localhost:8090"
echo "RedisInsight: http://localhost:5540"
echo "Adminer:      http://localhost:8091"
echo "Logs:         tail -f logs/booking-service.log"
echo "Stop:         ./stop-all.sh"
