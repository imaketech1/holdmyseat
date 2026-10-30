package xyz.imaketech.holdmyseat.booking.hold;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Keys hold:{showId}:{seatId} (one cluster slot per show); value is holdId or "booked:<bookingId>". */
@Service
public class SeatHoldService {

    // All-or-nothing: returns the 1-based index of a taken seat, or 0 after setting all keys.
    private static final RedisScript<Long> HOLD = new DefaultRedisScript<>("""
            for i, key in ipairs(KEYS) do
              if redis.call('EXISTS', key) == 1 then return i end
            end
            for _, key in ipairs(KEYS) do
              redis.call('SET', key, ARGV[1], 'PX', ARGV[2])
            end
            return 0""", Long.class);

    // Compare-and-set on owner ARGV[1] (fencing); empty ARGV[2] deletes the key.
    private static final RedisScript<Long> COMPARE_AND_SET = new DefaultRedisScript<>("""
            local changed = 0
            for _, key in ipairs(KEYS) do
              if redis.call('GET', key) == ARGV[1] then
                if ARGV[2] == '' then redis.call('DEL', key) else redis.call('SET', key, ARGV[2]) end
                changed = changed + 1
              end
            end
            return changed""", Long.class);

    private final StringRedisTemplate redis;

    public SeatHoldService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** @return empty on success, or the first seat already taken. */
    public Optional<String> tryHold(UUID showId, List<String> seatIds, UUID holdId, Duration ttl) {
        Long result = redis.execute(HOLD, keys(showId, seatIds), holdId.toString(), String.valueOf(ttl.toMillis()));
        return result == null || result == 0 ? Optional.empty() : Optional.of(seatIds.get(result.intValue() - 1));
    }

    public void releaseHold(UUID showId, List<String> seatIds, UUID holdId) {
        redis.execute(COMPARE_AND_SET, keys(showId, seatIds), holdId.toString(), "");
    }

    /** Turns holds into permanent "booked" markers. */
    public void markBooked(UUID showId, List<String> seatIds, UUID holdId, UUID bookingId) {
        redis.execute(COMPARE_AND_SET, keys(showId, seatIds), holdId.toString(), "booked:" + bookingId);
    }

    public void releaseBooked(UUID showId, List<String> seatIds, UUID bookingId) {
        redis.execute(COMPARE_AND_SET, keys(showId, seatIds), "booked:" + bookingId, "");
    }

    private static List<String> keys(UUID showId, List<String> seatIds) {
        return seatIds.stream().map(seat -> "hold:{" + showId + "}:" + seat).toList();
    }
}
