package xyz.imaketech.holdmyseat.booking.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Writes events to the outbox in the caller's transaction; OutboxRelay ships them to Kafka. */
@Component
public class EventPublisher {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public EventPublisher(OutboxRepository outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, UUID aggregateId, String type, Map<String, ?> payload) {
        UUID eventId = UUID.randomUUID();
        ObjectNode envelope = mapper.createObjectNode()
                .put("eventId", eventId.toString())
                .put("type", type)
                .put("aggregateId", aggregateId.toString())
                .put("occurredAt", Instant.now().toString());
        envelope.set("payload", mapper.valueToTree(payload));
        outbox.save(new OutboxEvent(eventId, topic, aggregateId.toString(), type, envelope.toString()));
    }
}
