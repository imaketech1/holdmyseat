package xyz.imaketech.holdmyseat.booking.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import xyz.imaketech.holdmyseat.booking.catalog.CatalogClient;
import xyz.imaketech.holdmyseat.booking.catalog.CatalogClient.CatalogShow;
import xyz.imaketech.holdmyseat.booking.config.BookingProperties;
import xyz.imaketech.holdmyseat.booking.domain.*;
import xyz.imaketech.holdmyseat.booking.hold.SeatHoldService;
import xyz.imaketech.holdmyseat.booking.messaging.EventEnvelope;
import xyz.imaketech.holdmyseat.booking.messaging.ProcessedEvent;
import xyz.imaketech.holdmyseat.booking.messaging.ProcessedEventRepository;
import xyz.imaketech.holdmyseat.booking.outbox.EventPublisher;
import xyz.imaketech.holdmyseat.booking.web.Dtos.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static xyz.imaketech.holdmyseat.booking.config.KafkaConfig.BOOKING_EVENTS;

/** Redis holds reject contention fast; a DB partial unique index is the final guard. */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookings;
    private final BookingSeatRepository seats;
    private final ProcessedEventRepository processed;
    private final SeatHoldService holds;
    private final CatalogClient catalog;
    private final EventPublisher events;
    private final BookingProperties props;
    private final TransactionTemplate tx;

    public BookingService(BookingRepository bookings, BookingSeatRepository seats,
                          ProcessedEventRepository processed, SeatHoldService holds, CatalogClient catalog,
                          EventPublisher events, BookingProperties props, TransactionTemplate tx) {
        this.bookings = bookings;
        this.seats = seats;
        this.processed = processed;
        this.holds = holds;
        this.catalog = catalog;
        this.events = events;
        this.props = props;
        this.tx = tx;
    }

    public BookingResponse hold(UUID userId, HoldRequest req) {
        List<String> seatIds = req.seatIds().stream().distinct().toList();
        if (seatIds.size() > props.maxSeatsPerHold()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max " + props.maxSeatsPerHold() + " seats per booking");
        }
        CatalogShow show = catalog.getShow(req.showId());
        if (show.startsAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Show has already started");
        }
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        for (String seat : seatIds) {
            BigDecimal price = show.seatPrices().get(seat);
            if (price == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown seat " + seat);
            }
            prices.put(seat, price);
        }

        // Layer 1: Redis.
        UUID holdId = UUID.randomUUID();
        Optional<String> taken = holds.tryHold(req.showId(), seatIds, holdId, props.holdTtl());
        if (taken.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat " + taken.get() + " is no longer available");
        }

        // Layer 2: PostgreSQL unique index, the source of truth.
        try {
            Booking booking = tx.execute(status -> {
                Booking b = bookings.saveAndFlush(new Booking(userId, req.showId(), holdId, prices,
                        Instant.now().plus(props.holdTtl())));
                events.publish(BOOKING_EVENTS, b.getId(), "SeatsHeld", seatPayload(b));
                return b;
            });
            return BookingResponse.from(booking);
        } catch (DataIntegrityViolationException e) {
            holds.releaseHold(req.showId(), seatIds, holdId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more seats were just taken");
        } catch (RuntimeException e) {
            holds.releaseHold(req.showId(), seatIds, holdId);
            throw e;
        }
    }

    @Transactional
    public BookingResponse checkout(UUID userId, UUID bookingId) {
        Booking b = owned(userId, bookingId);
        if (b.getStatus() != BookingStatus.HELD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is " + b.getStatus());
        }
        if (b.isExpired()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hold has expired");
        }
        b.moveTo(BookingStatus.PAYMENT_PENDING);
        events.publish(BOOKING_EVENTS, b.getId(), "PaymentRequested", Map.of(
                "bookingId", b.getId(), "userId", b.getUserId(), "amount", b.getAmount()));
        return BookingResponse.from(b);
    }

    @Transactional
    public BookingResponse cancel(UUID userId, UUID bookingId) {
        Booking b = owned(userId, bookingId);
        switch (b.getStatus()) {
            case HELD -> {
                b.moveTo(BookingStatus.CANCELLED);
                afterCommit(() -> holds.releaseHold(b.getShowId(), b.seatIds(), b.getHoldId()));
            }
            case CONFIRMED -> {
                b.moveTo(BookingStatus.CANCELLED);
                afterCommit(() -> holds.releaseBooked(b.getShowId(), b.seatIds(), b.getId()));
                events.publish(BOOKING_EVENTS, b.getId(), "RefundRequested", Map.of(
                        "bookingId", b.getId(), "userId", b.getUserId(), "amount", b.getAmount(),
                        "reason", "USER_CANCELLED"));
            }
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel a booking that is " + b.getStatus());
        }
        publishReleased(b, "USER_CANCELLED");
        return BookingResponse.from(b);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UUID userId, UUID bookingId) {
        return BookingResponse.from(owned(userId, bookingId));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine(UUID userId) {
        return bookings.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream().map(BookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Availability availability(UUID showId) {
        List<String> held = new ArrayList<>();
        List<String> booked = new ArrayList<>();
        for (BookingSeat s : seats.findByShowAndStatus(showId, List.of(SeatStatus.HELD, SeatStatus.CONFIRMED))) {
            (s.getStatus() == SeatStatus.CONFIRMED ? booked : held).add(s.getSeatId());
        }
        return new Availability(showId, held, booked);
    }

    @Transactional
    public void handlePaymentEvent(EventEnvelope event) {
        if (processed.existsById(event.eventId())) {
            return; // duplicate delivery
        }
        processed.save(new ProcessedEvent(event.eventId()));

        UUID bookingId = UUID.fromString(event.payload().path("bookingId").asText());
        Booking b = bookings.findById(bookingId).orElse(null);
        if (b == null) {
            log.warn("Payment event {} for unknown booking {}", event.type(), bookingId);
            return;
        }
        switch (event.type()) {
            case "PaymentSucceeded" -> onPaymentSucceeded(b);
            case "PaymentFailed" -> onPaymentFailed(b, event.payload().path("reason").asText("DECLINED"));
            default -> { /* RefundCompleted etc. are not Booking's concern */ }
        }
    }

    /** Confirms if still awaiting payment; otherwise refunds. */
    private void onPaymentSucceeded(Booking b) {
        if (b.getStatus() == BookingStatus.PAYMENT_PENDING) {
            b.moveTo(BookingStatus.CONFIRMED);
            afterCommit(() -> holds.markBooked(b.getShowId(), b.seatIds(), b.getHoldId(), b.getId()));
            Map<String, Object> payload = new HashMap<>(seatPayload(b));
            payload.put("userId", b.getUserId());
            payload.put("amount", b.getAmount());
            events.publish(BOOKING_EVENTS, b.getId(), "BookingConfirmed", payload);
        } else if (b.getStatus() != BookingStatus.CONFIRMED) {
            events.publish(BOOKING_EVENTS, b.getId(), "RefundRequested", Map.of(
                    "bookingId", b.getId(), "userId", b.getUserId(), "amount", b.getAmount(),
                    "reason", "LATE_PAYMENT_BOOKING_" + b.getStatus()));
        }
    }

    private void onPaymentFailed(Booking b, String reason) {
        if (b.getStatus() != BookingStatus.PAYMENT_PENDING) {
            return;
        }
        b.moveTo(BookingStatus.FAILED);
        afterCommit(() -> holds.releaseHold(b.getShowId(), b.seatIds(), b.getHoldId()));
        publishReleased(b, "PAYMENT_FAILED: " + reason);
    }

    /** Backstop for abandoned holds: frees DB rows after the Redis TTL. */
    @Transactional
    public int expireOverdue() {
        List<Booking> overdue = bookings.lockOverdue(200);
        for (Booking b : overdue) {
            b.moveTo(BookingStatus.EXPIRED);
            afterCommit(() -> holds.releaseHold(b.getShowId(), b.seatIds(), b.getHoldId()));
            publishReleased(b, "EXPIRED");
        }
        return overdue.size();
    }

    private Booking owned(UUID userId, UUID bookingId) {
        return bookings.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private void publishReleased(Booking b, String reason) {
        events.publish(BOOKING_EVENTS, b.getId(), "SeatsReleased", seatPayload(b));
        events.publish(BOOKING_EVENTS, b.getId(), "BookingCancelled", Map.of(
                "bookingId", b.getId(), "userId", b.getUserId(), "reason", reason));
    }

    private static Map<String, Object> seatPayload(Booking b) {
        return Map.of("bookingId", b.getId(), "showId", b.getShowId(), "seatIds", b.seatIds());
    }

    /** Applies Redis changes only after the DB commit. */
    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException e) {
                    log.warn("Redis hold update failed after commit; DB constraint still guards seats", e);
                }
            }
        });
    }
}
