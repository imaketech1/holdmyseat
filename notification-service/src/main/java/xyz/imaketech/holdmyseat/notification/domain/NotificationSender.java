package xyz.imaketech.holdmyseat.notification.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Stub that logs; throwing triggers Kafka retries, then the DLT. */
@Component
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    public void send(Notification.Channel channel, UUID userId, String body) {
        log.info("[{}] to user {}: {}", channel, userId, body);
    }
}
