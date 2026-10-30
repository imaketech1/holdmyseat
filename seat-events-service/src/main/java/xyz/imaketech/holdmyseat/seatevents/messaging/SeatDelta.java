package xyz.imaketech.holdmyseat.seatevents.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** What the browser receives: only the seats that changed, never the whole map. */
public record SeatDelta(UUID showId, List<String> seatIds, SeatState state, Instant at) {

    public enum SeatState { HELD, BOOKED, AVAILABLE }
}
