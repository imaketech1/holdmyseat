#!/usr/bin/env bash
# End-to-end test via the gateway; needs curl, jq and all services running.
set -euo pipefail

GW=http://localhost:8080
SHOW=33333333-3333-3333-3333-333333333333   # seeded demo show
RUN=$RANDOM

step() { printf '\n\033[1m== %s\033[0m\n' "$1"; }

register_and_login() {
  curl -sf -X POST $GW/api/auth/register -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"password123\",\"fullName\":\"Test User\"}" > /dev/null
  curl -sf -X POST $GW/api/auth/login -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"password123\"}" | jq -r .accessToken
}

step "1. Register + login two users"
T1=$(register_and_login "alice$RUN@test.com")
T2=$(register_and_login "bob$RUN@test.com")
echo "alice and bob logged in"

step "2. Browse shows (public, cached in Redis)"
curl -sf "$GW/api/catalog/shows?city=Hyderabad" | jq '.[] | {id, title, startsAt}'

SEAT1="J$((RANDOM % 15 + 1))"
SEAT2="I$((RANDOM % 15 + 1))"

step "3. Alice holds $SEAT1 and $SEAT2"
BOOKING=$(curl -sf -X POST $GW/api/holds -H "Authorization: Bearer $T1" -H 'Content-Type: application/json' \
  -d "{\"showId\":\"$SHOW\",\"seatIds\":[\"$SEAT1\",\"$SEAT2\"]}")
echo "$BOOKING" | jq '{id, seats, amount, status, expiresAt}'
BID=$(echo "$BOOKING" | jq -r .id)

step "4. Bob tries the same seat -> expect 409"
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $GW/api/holds -H "Authorization: Bearer $T2" \
  -H 'Content-Type: application/json' -d "{\"showId\":\"$SHOW\",\"seatIds\":[\"$SEAT1\"]}"

step "5. Alice checks out (starts the Kafka saga)"
curl -sf -X POST $GW/api/bookings/$BID/checkout -H "Authorization: Bearer $T1" | jq '{id, status}'

step "6. Wait for payment, then check status (CONFIRMED, or FAILED ~10% of the time)"
sleep 3
curl -sf $GW/api/bookings/$BID -H "Authorization: Bearer $T1" | jq '{id, status}'
curl -sf $GW/api/payments/bookings/$BID -H "Authorization: Bearer $T1" | jq '{status, amount, failureReason}'

step "7. Seat map availability"
curl -sf $GW/api/bookings/shows/$SHOW/availability -H "Authorization: Bearer $T1" | jq .

step "8. Race test: 20 parallel holds on one seat -> expect exactly one 201"
RACE_SEAT="H$((RANDOM % 15 + 1))"
for i in $(seq 1 20); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST $GW/api/holds -H "Authorization: Bearer $T2" \
    -H 'Content-Type: application/json' -d "{\"showId\":\"$SHOW\",\"seatIds\":[\"$RACE_SEAT\"]}" &
done | sort | uniq -c
wait

echo -e "\nDone. Check the notification-service console for the [EMAIL]/[SMS] log lines."
