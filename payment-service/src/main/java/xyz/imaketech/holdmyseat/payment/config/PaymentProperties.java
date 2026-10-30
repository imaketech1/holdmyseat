package xyz.imaketech.holdmyseat.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Simulated failures and declines to exercise the saga's failure paths. */
@ConfigurationProperties(prefix = "holdmyseat.payment")
public record PaymentProperties(double failureRate, BigDecimal maxAmount) {
}
