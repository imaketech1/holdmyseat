package xyz.imaketech.holdmyseat.payment.domain;

import org.springframework.stereotype.Component;
import xyz.imaketech.holdmyseat.payment.config.PaymentProperties;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Stand-in for a real payment provider. */
@Component
public class SimulatedPaymentProvider {

    public record Result(boolean success, String providerRef, String failureReason) {
    }

    private final PaymentProperties props;

    public SimulatedPaymentProvider(PaymentProperties props) {
        this.props = props;
    }

    public Result charge(UUID idempotencyKey, BigDecimal amount) {
        if (amount.compareTo(props.maxAmount()) > 0) {
            return new Result(false, null, "AMOUNT_LIMIT_EXCEEDED");
        }
        if (ThreadLocalRandom.current().nextDouble() < props.failureRate()) {
            return new Result(false, null, "CARD_DECLINED");
        }
        return new Result(true, "sim_" + idempotencyKey.toString().substring(0, 8), null);
    }
}
