CREATE TABLE payments (
    id             UUID PRIMARY KEY,
    booking_id     UUID           NOT NULL UNIQUE, -- idempotency key
    user_id        UUID           NOT NULL,
    amount         NUMERIC(10, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    failure_reason VARCHAR(100),
    provider_ref   VARCHAR(100),
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE outbox (
    id           UUID PRIMARY KEY,
    topic        VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(64)  NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox (created_at) WHERE published_at IS NULL;

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
