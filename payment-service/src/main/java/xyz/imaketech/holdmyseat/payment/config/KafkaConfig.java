package xyz.imaketech.holdmyseat.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

    public static final String BOOKING_EVENTS = "booking.events";
    public static final String PAYMENT_EVENTS = "payment.events";

    @Bean
    NewTopic paymentEvents() {
        return TopicBuilder.name(PAYMENT_EVENTS).partitions(6).replicas(1).build();
    }

    @Bean
    NewTopic bookingEventsDlt() {
        return TopicBuilder.name(BOOKING_EVENTS + ".payment.DLT").partitions(6).replicas(1).build();
    }

    /** Retries 1s, 2s, 4s, then parks the message on booking.events.payment.DLT. */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".payment.DLT", record.partition()));
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
