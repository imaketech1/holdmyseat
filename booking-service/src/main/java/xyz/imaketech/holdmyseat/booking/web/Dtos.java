package xyz.imaketech.holdmyseat.booking.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import xyz.imaketech.holdmyseat.booking.domain.Booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Dtos {

    private Dtos() {
    }

    public record HoldRequest(@NotNull UUID showId, @NotEmpty List<String> seatIds) {
    }

    public record BookingResponse(UUID id, UUID showId, List<String> seats, BigDecimal amount,
                                  String status, Instant expiresAt, Instant createdAt) {
        public static BookingResponse from(Booking b) {
            return new BookingResponse(b.getId(), b.getShowId(), b.seatIds(), b.getAmount(),
                    b.getStatus().name(), b.getExpiresAt(), b.getCreatedAt());
        }
    }

    public record Availability(UUID showId, List<String> held, List<String> booked) {
    }
}
