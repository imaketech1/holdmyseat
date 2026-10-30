package xyz.imaketech.holdmyseat.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import xyz.imaketech.holdmyseat.payment.config.KafkaConfig;
import xyz.imaketech.holdmyseat.payment.web.PaymentService;

@Component
public class BookingEventsListener {

    private final ObjectMapper mapper;
    private final PaymentService payments;

    public BookingEventsListener(ObjectMapper mapper, PaymentService payments) {
        this.mapper = mapper;
        this.payments = payments;
    }

    @KafkaListener(topics = KafkaConfig.BOOKING_EVENTS, groupId = "payment-service")
    public void onMessage(String message) throws Exception {
        EventEnvelope event = mapper.readValue(message, EventEnvelope.class);
        if ("PaymentRequested".equals(event.type()) || "RefundRequested".equals(event.type())) {
            payments.handleBookingEvent(event);
        }
    }
}
