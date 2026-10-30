-- 3 Idiots today 7 PM IST, Interstellar tomorrow 4 PM IST (relative to run date).
UPDATE shows
SET starts_at = ((now() AT TIME ZONE 'Asia/Kolkata')::date + interval '19 hours') AT TIME ZONE 'Asia/Kolkata'
WHERE id = '44444444-4444-4444-4444-444444444444';

UPDATE shows
SET starts_at = ((now() AT TIME ZONE 'Asia/Kolkata')::date + interval '1 day 16 hours') AT TIME ZONE 'Asia/Kolkata'
WHERE id = '33333333-3333-3333-3333-333333333333';
