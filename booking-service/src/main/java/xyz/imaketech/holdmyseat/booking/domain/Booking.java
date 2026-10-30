package xyz.imaketech.holdmyseat.booking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    /** Redis hold value; doubles as the fencing token. */
    @Column(name = "hold_id", nullable = false)
    private UUID holdId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.HELD;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Version
    private Long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> seats = new ArrayList<>();

    protected Booking() {
    }

    public Booking(UUID userId, UUID showId, UUID holdId, Map<String, BigDecimal> seatPrices, Instant expiresAt) {
        this.userId = userId;
        this.showId = showId;
        this.holdId = holdId;
        this.expiresAt = expiresAt;
        seatPrices.forEach((seat, price) -> seats.add(new BookingSeat(this, seat, price)));
        this.amount = seatPrices.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void moveTo(BookingStatus next) {
        this.status = next;
        SeatStatus seatStatus = switch (next) {
            case CONFIRMED -> SeatStatus.CONFIRMED;
            case CANCELLED, FAILED, EXPIRED -> SeatStatus.RELEASED;
            case HELD, PAYMENT_PENDING -> SeatStatus.HELD;
        };
        seats.forEach(s -> s.setStatus(seatStatus));
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public List<String> seatIds() {
        return seats.stream().map(BookingSeat::getSeatId).toList();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getShowId() { return showId; }
    public UUID getHoldId() { return holdId; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
