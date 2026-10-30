package xyz.imaketech.holdmyseat.notification.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** event_id + channel is unique, so redeliveries never notify twice. */
@Entity
@Table(name = "notifications")
public class Notification {

    public enum Channel { EMAIL, SMS }

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private String template;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    protected Notification() {
    }

    public Notification(UUID eventId, UUID userId, UUID bookingId, Channel channel, String template, String body) {
        this.eventId = eventId;
        this.userId = userId;
        this.bookingId = bookingId;
        this.channel = channel;
        this.template = template;
        this.body = body;
    }
}
