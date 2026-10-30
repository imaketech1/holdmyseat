package xyz.imaketech.holdmyseat.booking.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import xyz.imaketech.holdmyseat.booking.config.KafkaConfig;
import xyz.imaketech.holdmyseat.booking.web.BookingService;

@Component
public class PaymentEventsListener {

    private final ObjectMapper mapper;
    private final BookingService bookings;

    public PaymentEventsListener(ObjectMapper mapper, BookingService bookings) {
        this.mapper = mapper;
        this.bookings = bookings;
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_EVENTS, groupId = "booking-service")
    public void onMessage(String message) throws Exception {
        bookings.handlePaymentEvent(mapper.readValue(message, EventEnvelope.class));
    }
}
