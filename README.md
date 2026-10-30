# HoldMySeat

**Real-time seat booking with distributed locking**

**Tech:** Java 21 · Spring Boot 3 · Spring Cloud Gateway · Spring Security (JWT, RS256) · Spring Data JPA · Apache Kafka · Redis · PostgreSQL · Flyway · Resilience4j · WebSocket (STOMP) · Docker · React + TypeScript (Vite)

A microservices movie-ticket booking platform built so that the same seat can never be sold twice, even when thousands of users click it at once. Seven Spring Boot services sit behind an API gateway: Auth, Catalog, Booking, Payment, Notification and Seat Events. Holds are instant, payment runs as a Kafka saga, and every open seat map updates live over WebSockets.

## How seat locking works

A seat is protected by two independent locks, so a failure in one still can't cause a double booking.

**1. Redis: an atomic, all-or-nothing hold.** Each seat is a key `hold:{showId}:{seatId}` with a 7-minute TTL. One Lua script claims every requested seat or none of them. Redis runs it atomically, so there's no gap between "is it free?" and "take it".

```lua
for i, key in ipairs(KEYS) do
  if redis.call('EXISTS', key) == 1 then return i end   -- seat taken: claim nothing
end
for _, key in ipairs(KEYS) do
  redis.call('SET', key, ARGV[1], 'PX', ARGV[2])         -- claim all, with TTL
end
return 0
```

Losers get a `409` in under a millisecond, without touching the database. Releasing a hold only deletes keys that still carry *your* hold ID, so a slow request can't free someone else's seat.

**2. PostgreSQL: the source of truth.** Redis can lose a key on failover, so every hold is also written to the database, guarded by a partial unique index:

```sql
CREATE UNIQUE INDEX uq_active_seat ON booking_seats (show_id, seat_id)
    WHERE status IN ('HELD', 'CONFIRMED');
```

If two requests ever get past Redis, the second insert fails and that request is rejected.

**Expiry and payment.** Abandoned holds expire through the Redis TTL, and a sweeper releases the matching database rows. Checkout starts a Kafka saga with Payment. If payment fails, the seats are released; if payment arrives after the hold expired, the money is refunded automatically. All of it uses a transactional outbox and idempotent consumers.


## Run it locally

**Requirements:** Docker, JDK 21, Maven, Node.js 18+

**1. Start the backend** (Postgres, Redis, Kafka and all 7 services):

```bash
./start-all.sh
```

This builds the services, starts them in the background and waits until each one is healthy. Logs are in `logs/`.

**2. Start the frontend** in a second terminal:

```bash
cd holdmyseat-web
npm install
npm run dev
```

Open **http://localhost:5173**, create an account, and open the demo show in Hyderabad. To see live updates, open the same show in a private window with a second account and hold a seat.
After the first `npm install`, `./start-all.sh` also starts the frontend for you, so you only need `npm run dev` the first time.

**Stop:** `./stop-all.sh` stops the services; `./stop-all.sh --all` also stops Docker.

**Test the flow and race condition:** `./test-flow.sh` registers users, books seats and fires 20 parallel holds at one seat. Exactly one should succeed.

| Service | Port |
| --- | --- |
| API Gateway | 8080 |
| Auth / Catalog / Booking / Payment / Notification / Seat Events | 8081–8086 |
| Kafka UI / Adminer / RedisInsight | 8090 / 8091 / 5540 |
