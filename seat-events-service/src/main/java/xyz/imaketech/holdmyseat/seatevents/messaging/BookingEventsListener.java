package xyz.imaketech.holdmyseat.seatevents.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import xyz.imaketech.holdmyseat.seatevents.messaging.SeatDelta.SeatState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Unique consumer group per instance so every instance reaches its own clients. */
@Component
public class BookingEventsListener {

    private final ObjectMapper mapper;
    private final SimpMessagingTemplate broker;

    public BookingEventsListener(ObjectMapper mapper, SimpMessagingTemplate broker) {
        this.mapper = mapper;
        this.broker = broker;
    }

    @KafkaListener(topics = "booking.events",
            groupId = "seat-events-#{T(java.util.UUID).randomUUID().toString()}",
            properties = "auto.offset.reset=latest")
    public void onMessage(String message) throws Exception {
        JsonNode event = mapper.readTree(message);
        SeatState state = switch (event.path("type").asText()) {
            case "SeatsHeld" -> SeatState.HELD;
            case "BookingConfirmed" -> SeatState.BOOKED;
            case "SeatsReleased" -> SeatState.AVAILABLE;
            default -> null;
        };
        if (state == null) {
            return;
        }
        JsonNode p = event.path("payload");
        UUID showId = UUID.fromString(p.path("showId").asText());
        List<String> seats = new ArrayList<>();
        p.path("seatIds").forEach(s -> seats.add(s.asText()));

        broker.convertAndSend("/topic/shows/" + showId, new SeatDelta(showId, seats, state, Instant.now()));
    }
}
