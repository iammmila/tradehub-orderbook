package com.ab.tradeservice.config;

import com.ab.tradeservice.kafka.event.TradeCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer wiring for TradeCreatedEvent.
 *
 * Usage:
 * - Provides the container factory referenced by @KafkaListener so this service can consume JSON events.
 * - Centralizes deserialization rules to avoid per-listener duplication and reduce runtime surprises.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeCreatedEvent> kafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap,
            @Value("${spring.kafka.consumer.group-id}") String groupId
    ) {
        Map<String, Object> props = new HashMap<>();

        // points the consumer to the Kafka cluster and identifies this service's consumer group.
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // When a new environment/topic is used (or offsets are lost), starting from earliest helps
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        //we want strong typing (TradeCreatedEvent) and JSON payloads from producers.
        JacksonJsonDeserializer<TradeCreatedEvent> valueDeserializer =
                new JacksonJsonDeserializer<>(TradeCreatedEvent.class);

        // trusted packages is a safety measure for JSON type mapping
        valueDeserializer.addTrustedPackages("com.ab.tradeservice.kafka.event");

        // disable type headers so producers don’t need to send Spring-specific type metadata.
        // This prevents cross-service coupling and avoids "type header missing/mismatch" issues.
        valueDeserializer.setUseTypeHeaders(false);

        var consumerFactory = new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
        //container factory controls listener concurrency/ack/error behavior in one place.
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TradeCreatedEvent>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}