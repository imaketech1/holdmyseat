CREATE TABLE bookings (
    id         UUID PRIMARY KEY,
    user_id    UUID           NOT NULL,
    show_id    UUID           NOT NULL,
    hold_id    UUID           NOT NULL,
    status     VARCHAR(20)    NOT NULL,
    amount     NUMERIC(10, 2) NOT NULL,
    expires_at TIMESTAMPTZ    NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL,
    version    BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_bookings_user ON bookings (user_id, created_at DESC);
CREATE INDEX idx_bookings_overdue ON bookings (expires_at) WHERE status IN ('HELD', 'PAYMENT_PENDING');

CREATE TABLE booking_seats (
    id         UUID PRIMARY KEY,
    booking_id UUID           NOT NULL REFERENCES bookings (id),
    show_id    UUID           NOT NULL,
    seat_id    VARCHAR(10)    NOT NULL,
    price      NUMERIC(10, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL
);

-- Final guard against double booking: one active row per seat per show.
CREATE UNIQUE INDEX uq_active_seat ON booking_seats (show_id, seat_id) WHERE status IN ('HELD', 'CONFIRMED');
CREATE INDEX idx_booking_seats_booking ON booking_seats (booking_id);

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
