package xyz.imaketech.holdmyseat.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "holdmyseat.booking")
public record BookingProperties(Duration holdTtl, int maxSeatsPerHold, String catalogUri) {
}
