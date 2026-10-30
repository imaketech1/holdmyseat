package xyz.imaketech.holdmyseat.notification.domain;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.imaketech.holdmyseat.notification.messaging.EventEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository log;
    private final NotificationSender sender;

    public NotificationService(NotificationRepository log, NotificationSender sender) {
        this.log = log;
        this.sender = sender;
    }

    @Transactional
    public void handle(EventEnvelope event) {
        String body = render(event.type(), event.payload());
        if (body == null) {
            return; // not an event we notify about
        }
        JsonNode p = event.payload();
        UUID userId = UUID.fromString(p.path("userId").asText());
        UUID bookingId = UUID.fromString(p.path("bookingId").asText());

        for (Notification.Channel channel : Notification.Channel.values()) {
            if (log.existsByEventIdAndChannel(event.eventId(), channel)) {
                continue; // already sent on an earlier delivery
            }
            sender.send(channel, userId, body);
            log.save(new Notification(event.eventId(), userId, bookingId, channel, event.type(), body));
        }
    }

    private static String render(String type, JsonNode p) {
        String booking = p.path("bookingId").asText();
        return switch (type) {
            case "BookingConfirmed" -> "Your booking " + shortId(booking) + " is confirmed. Seats: "
                    + seats(p) + ". Amount paid: " + p.path("amount").asText() + ".";
            case "BookingCancelled" -> "Your booking " + shortId(booking) + " was cancelled ("
                    + reason(p.path("reason").asText()) + "). The seats have been released.";
            case "RefundCompleted" -> "A refund of " + p.path("amount").asText() + " for booking "
                    + shortId(booking) + " has been processed.";
            default -> null;
        };
    }

    private static String seats(JsonNode p) {
        List<String> seats = new ArrayList<>();
        p.path("seatIds").forEach(s -> seats.add(s.asText()));
        return String.join(", ", seats);
    }

    private static String reason(String raw) {
        if (raw.startsWith("PAYMENT_FAILED")) return "payment failed";
        if (raw.equals("EXPIRED")) return "hold expired";
        if (raw.equals("USER_CANCELLED")) return "cancelled by you";
        return raw.toLowerCase();
    }

    private static String shortId(String id) {
        return id.length() > 8 ? id.substring(0, 8).toUpperCase() : id;
    }
}
