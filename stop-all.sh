#!/usr/bin/env bash
# Usage: ./stop-all.sh [--all]   (--all also stops Docker infra)
cd "$(dirname "$0")"

for f in .pids/*; do
  [ -f "$f" ] || continue
  pid=$(cat "$f")
  if kill -0 "$pid" 2>/dev/null; then
    # web app runs in its own process group
    { kill -- -"$pid" 2>/dev/null || kill "$pid"; } && echo "stopped $(basename "$f")"
  fi
  rm -f "$f"
done

if [ "${1:-}" = "--all" ]; then
  docker compose stop
fi
