package xyz.imaketech.holdmyseat.payment.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.imaketech.holdmyseat.payment.domain.*;
import xyz.imaketech.holdmyseat.payment.messaging.EventEnvelope;
import xyz.imaketech.holdmyseat.payment.messaging.ProcessedEvent;
import xyz.imaketech.holdmyseat.payment.messaging.ProcessedEventRepository;
import xyz.imaketech.holdmyseat.payment.outbox.EventPublisher;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static xyz.imaketech.holdmyseat.payment.config.KafkaConfig.PAYMENT_EVENTS;

/** Saga participant: charges on PaymentRequested, refunds on RefundRequested. */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final ProcessedEventRepository processed;
    private final SimulatedPaymentProvider provider;
    private final EventPublisher events;

    public PaymentService(PaymentRepository payments, ProcessedEventRepository processed,
                          SimulatedPaymentProvider provider, EventPublisher events) {
        this.payments = payments;
        this.processed = processed;
        this.provider = provider;
        this.events = events;
    }

    @Transactional
    public void handleBookingEvent(EventEnvelope event) {
        if (processed.existsById(event.eventId())) {
            return; // duplicate delivery
        }
        processed.save(new ProcessedEvent(event.eventId()));

        JsonNode p = event.payload();
        UUID bookingId = UUID.fromString(p.path("bookingId").asText());
        switch (event.type()) {
            case "PaymentRequested" -> charge(bookingId, UUID.fromString(p.path("userId").asText()),
                    new BigDecimal(p.path("amount").asText()));
            case "RefundRequested" -> refund(bookingId, p.path("reason").asText("UNSPECIFIED"));
            default -> { }
        }
    }

    private void charge(UUID bookingId, UUID userId, BigDecimal amount) {
        if (payments.findByBookingId(bookingId).isPresent()) {
            log.info("Booking {} already charged; ignoring repeat request", bookingId);
            return;
        }
        SimulatedPaymentProvider.Result result = provider.charge(bookingId, amount);
        Payment payment = payments.save(new Payment(bookingId, userId, amount, result));

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", bookingId);
        payload.put("paymentId", payment.getId());
        payload.put("amount", amount);
        if (result.success()) {
            events.publish(PAYMENT_EVENTS, bookingId, "PaymentSucceeded", payload);
        } else {
            payload.put("reason", result.failureReason());
            events.publish(PAYMENT_EVENTS, bookingId, "PaymentFailed", payload);
        }
    }

    private void refund(UUID bookingId, String reason) {
        Payment payment = payments.findByBookingId(bookingId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.SUCCEEDED) {
            log.warn("Refund requested for booking {} with no successful payment", bookingId);
            return;
        }
        payment.refund();
        events.publish(PAYMENT_EVENTS, bookingId, "RefundCompleted", Map.of(
                "bookingId", bookingId, "paymentId", payment.getId(), "amount", payment.getAmount(),
                "userId", payment.getUserId(), "reason", reason));
    }

    @Transactional(readOnly = true)
    public PaymentResponse forBooking(UUID userId, UUID bookingId) {
        return payments.findByBookingId(bookingId)
                .filter(pay -> pay.getUserId().equals(userId))
                .map(pay -> new PaymentResponse(pay.getId(), pay.getBookingId(), pay.getAmount(),
                        pay.getStatus().name(), pay.getFailureReason(), pay.getCreatedAt()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    public record PaymentResponse(UUID id, UUID bookingId, BigDecimal amount, String status,
                                  String failureReason, java.time.Instant createdAt) {
    }
}
