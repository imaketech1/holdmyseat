package xyz.imaketech.holdmyseat.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    boolean existsByEventIdAndChannel(UUID eventId, Notification.Channel channel);
}
