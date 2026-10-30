CREATE TABLE venues (
    id   UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL
);

CREATE TABLE screens (
    id       UUID PRIMARY KEY,
    venue_id UUID         NOT NULL REFERENCES venues (id),
    name     VARCHAR(100) NOT NULL,
    layout   JSONB        NOT NULL
);

CREATE TABLE shows (
    id         UUID PRIMARY KEY,
    screen_id  UUID          NOT NULL REFERENCES screens (id),
    title      VARCHAR(255)  NOT NULL,
    language   VARCHAR(50)   NOT NULL,
    starts_at  TIMESTAMPTZ   NOT NULL,
    base_price NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_venues_city ON venues (lower(city));
CREATE INDEX idx_shows_starts_at ON shows (starts_at);

-- Demo data: one venue, one 10-row screen, one show a week out.
INSERT INTO venues (id, name, city)
VALUES ('11111111-1111-1111-1111-111111111111', 'Galaxy Cinemas', 'Hyderabad');

INSERT INTO screens (id, venue_id, name, layout)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Screen 1',
        '[{"row":"A","seats":15,"category":"PREMIUM","priceMultiplier":1.5},
          {"row":"B","seats":15,"category":"PREMIUM","priceMultiplier":1.5},
          {"row":"C","seats":15,"category":"GOLD","priceMultiplier":1.2},
          {"row":"D","seats":15,"category":"GOLD","priceMultiplier":1.2},
          {"row":"E","seats":15,"category":"GOLD","priceMultiplier":1.2},
          {"row":"F","seats":15,"category":"SILVER","priceMultiplier":1.0},
          {"row":"G","seats":15,"category":"SILVER","priceMultiplier":1.0},
          {"row":"H","seats":15,"category":"SILVER","priceMultiplier":1.0},
          {"row":"I","seats":15,"category":"SILVER","priceMultiplier":1.0},
          {"row":"J","seats":15,"category":"SILVER","priceMultiplier":1.0}]');

INSERT INTO shows (id, screen_id, title, language, starts_at, base_price)
VALUES ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222',
        'Interstellar (Re-release)', 'English', now() + interval '7 days', 200.00);
