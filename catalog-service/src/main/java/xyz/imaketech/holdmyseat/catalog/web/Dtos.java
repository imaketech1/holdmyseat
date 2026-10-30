package xyz.imaketech.holdmyseat.catalog.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import xyz.imaketech.holdmyseat.catalog.model.SeatRow;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cached DTOs implement Serializable because the Redis cache uses JDK serialization. */
public final class Dtos {

    private Dtos() {
    }

    public record ShowSummary(UUID id, String title, String language, Instant startsAt,
                              BigDecimal basePrice, String venueName, String city) implements Serializable {
    }

    public record ShowDetail(UUID id, String title, String language, Instant startsAt, BigDecimal basePrice,
                             String venueName, String screenName, List<SeatRow> layout) implements Serializable {
    }

    public record InternalShow(UUID id, Instant startsAt, Map<String, BigDecimal> seatPrices) implements Serializable {
    }

    public record CreateVenue(@NotBlank String name, @NotBlank String city) {
    }

    public record SeatRowRequest(@NotBlank @Pattern(regexp = "[A-Z]{1,2}") String row,
                                 @Min(1) @Max(60) int seats,
                                 @NotBlank String category,
                                 @NotNull @DecimalMin("0.5") BigDecimal priceMultiplier) {
    }

    public record CreateScreen(@NotNull UUID venueId, @NotBlank String name,
                               @NotEmpty List<@Valid SeatRowRequest> layout) {
    }

    public record CreateShow(@NotNull UUID screenId, @NotBlank String title, @NotBlank String language,
                             @NotNull @Future Instant startsAt, @NotNull @Positive BigDecimal basePrice) {
    }

    public record IdResponse(UUID id) {
    }
}
