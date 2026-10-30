CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    event_id   UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    booking_id UUID        NOT NULL,
    channel    VARCHAR(10) NOT NULL,
    template   VARCHAR(50) NOT NULL,
    body       TEXT        NOT NULL,
    sent_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_notification_event_channel UNIQUE (event_id, channel)
);

CREATE INDEX idx_notifications_user ON notifications (user_id, sent_at DESC);
