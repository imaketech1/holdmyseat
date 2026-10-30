package xyz.imaketech.holdmyseat.booking.config;

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
    NewTopic bookingEvents() {
        return TopicBuilder.name(BOOKING_EVENTS).partitions(6).replicas(1).build();
    }

    @Bean
    NewTopic paymentEventsDlt() {
        return TopicBuilder.name(PAYMENT_EVENTS + ".DLT").partitions(6).replicas(1).build();
    }

    /** Retries 1s, 2s, 4s, then parks on <topic>.DLT. */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff);
    }
}
