package xyz.imaketech.holdmyseat.booking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/** A partial unique index on active (show_id, seat_id) is the final double-booking guard. */
@Entity
@Table(name = "booking_seats")
public class BookingSeat {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "seat_id", nullable = false)
    private String seatId;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.HELD;

    protected BookingSeat() {
    }

    BookingSeat(Booking booking, String seatId, BigDecimal price) {
        this.booking = booking;
        this.showId = booking.getShowId();
        this.seatId = seatId;
        this.price = price;
    }

    public String getSeatId() { return seatId; }
    public BigDecimal getPrice() { return price; }
    public SeatStatus getStatus() { return status; }
    void setStatus(SeatStatus status) { this.status = status; }
}
