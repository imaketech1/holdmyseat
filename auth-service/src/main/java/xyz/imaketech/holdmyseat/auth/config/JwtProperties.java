package xyz.imaketech.holdmyseat.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "holdmyseat.jwt")
public record JwtProperties(String issuer, Duration accessTtl, Duration refreshTtl, String keyId) {
}
