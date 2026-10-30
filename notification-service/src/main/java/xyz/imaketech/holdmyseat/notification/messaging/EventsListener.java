package xyz.imaketech.holdmyseat.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import xyz.imaketech.holdmyseat.notification.config.KafkaConfig;
import xyz.imaketech.holdmyseat.notification.domain.NotificationService;

@Component
public class EventsListener {

    private final ObjectMapper mapper;
    private final NotificationService notifications;

    public EventsListener(ObjectMapper mapper, NotificationService notifications) {
        this.mapper = mapper;
        this.notifications = notifications;
    }

    @KafkaListener(topics = {KafkaConfig.BOOKING_EVENTS, KafkaConfig.PAYMENT_EVENTS}, groupId = "notification-service")
    public void onMessage(String message) throws Exception {
        notifications.handle(mapper.readValue(message, EventEnvelope.class));
    }
}
