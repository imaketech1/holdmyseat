-- Move Interstellar to 4:00 PM IST, keeping its date (V1 seeds it relative to now()).
UPDATE shows
SET starts_at = (date_trunc('day', starts_at AT TIME ZONE 'Asia/Kolkata') + interval '16 hours') AT TIME ZONE 'Asia/Kolkata'
WHERE id = '33333333-3333-3333-3333-333333333333';
