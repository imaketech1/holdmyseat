package xyz.imaketech.holdmyseat.booking.domain;

/** HELD → PAYMENT_PENDING → CONFIRMED; can end CANCELLED, FAILED or EXPIRED. */
public enum BookingStatus {
    HELD, PAYMENT_PENDING, CONFIRMED, CANCELLED, FAILED, EXPIRED
}
