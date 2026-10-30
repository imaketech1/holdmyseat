package xyz.imaketech.holdmyseat.booking.outbox;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID id, String topic, String aggregateId, String type, String payload) {
        this.id = id;
        this.topic = topic;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
    }

    public void markPublished() { this.publishedAt = Instant.now(); }
    public String getTopic() { return topic; }
    public String getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
}
