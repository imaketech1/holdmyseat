package xyz.imaketech.holdmyseat.booking.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/** At-least-once relay to Kafka keyed by aggregate id; consumers dedupe. */
@Component
public class OutboxRelay {

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(OutboxRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${holdmyseat.outbox.poll-ms:500}")
    @Transactional
    public void relay() {
        for (OutboxEvent event : outbox.lockUnpublished(100)) {
            try {
                kafka.send(event.getTopic(), event.getAggregateId(), event.getPayload()).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("Kafka send failed; batch will retry", e);
            }
            event.markPublished();
        }
    }
}
