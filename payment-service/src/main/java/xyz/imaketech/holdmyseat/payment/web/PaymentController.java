package xyz.imaketech.holdmyseat.payment.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import xyz.imaketech.holdmyseat.payment.web.PaymentService.PaymentResponse;

import java.util.UUID;

/** Payments are created only through the saga (Kafka); the REST API is read-only. */
@RestController
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @GetMapping("/api/payments/bookings/{bookingId}")
    public PaymentResponse forBooking(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID bookingId) {
        return payments.forBooking(userId, bookingId);
    }
}
