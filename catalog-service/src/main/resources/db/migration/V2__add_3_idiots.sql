-- Demo data: a second show on the same screen, starting a day before Interstellar so it lists first.
INSERT INTO shows (id, screen_id, title, language, starts_at, base_price)
VALUES ('44444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222',
        '3 Idiots', 'Hindi', now() + interval '6 days', 200.00);
