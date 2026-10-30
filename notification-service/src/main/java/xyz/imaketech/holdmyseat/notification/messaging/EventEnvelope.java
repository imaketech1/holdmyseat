package xyz.imaketech.holdmyseat.notification.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(UUID eventId, String type, String aggregateId, Instant occurredAt, JsonNode payload) {
}
