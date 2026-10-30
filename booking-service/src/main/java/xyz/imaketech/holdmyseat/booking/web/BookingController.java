package xyz.imaketech.holdmyseat.booking.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import xyz.imaketech.holdmyseat.booking.web.Dtos.*;

import java.util.List;
import java.util.UUID;

/** X-User-Id is set by the gateway from the validated JWT. */
@RestController
public class BookingController {

    private static final String USER = "X-User-Id";

    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @PostMapping("/api/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse hold(@RequestHeader(USER) UUID userId, @Valid @RequestBody HoldRequest req) {
        return bookings.hold(userId, req);
    }

    @PostMapping("/api/bookings/{id}/checkout")
    public BookingResponse checkout(@RequestHeader(USER) UUID userId, @PathVariable UUID id) {
        return bookings.checkout(userId, id);
    }

    @DeleteMapping("/api/bookings/{id}")
    public BookingResponse cancel(@RequestHeader(USER) UUID userId, @PathVariable UUID id) {
        return bookings.cancel(userId, id);
    }

    @GetMapping("/api/bookings")
    public List<BookingResponse> mine(@RequestHeader(USER) UUID userId) {
        return bookings.mine(userId);
    }

    @GetMapping("/api/bookings/{id}")
    public BookingResponse get(@RequestHeader(USER) UUID userId, @PathVariable UUID id) {
        return bookings.get(userId, id);
    }

    @GetMapping("/api/bookings/shows/{showId}/availability")
    public Availability availability(@PathVariable UUID showId) {
        return bookings.availability(showId);
    }
}
