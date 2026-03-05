package com.ab.notificationservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Usage:
 * - Kafka consumer configuration for notification-service listeners.
 * - Supports JSON events with class mapping across multiple services/packages.
 * - Uses error-handling deserializers to avoid consumer crash on bad payloads.
 */
@Configuration
public class KafkaConsumerConfig {

    /**
     * ConsumerFactory configured for:
     * - JSON deserialization (Jackson)
     * - Type mapping between producer event and local event classes
     * - ErrorHandlingDeserializer to capture deserialization problems cleanly
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        // Broker + consumer identity
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Wrap deserializers to surface deserialization errors (instead of killing the consumer thread)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);

        // Allow type headers and resolve into local classes
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        /**
         * Type mappings:
         * - Producers may publish events using their own package names.
         * - Consumer maps those external FQCNs to local DTO/event classes in this service.
         */
        props.put(JacksonJsonDeserializer.TYPE_MAPPINGS,
                "com.ab.tradeservice.kafka.event.TradeCreatedEvent:com.ab.notificationservice.kafka.event.TradeCreatedEvent," +
                        "com.ab.orderservice.kafka.event.TradeCreatedEvent:com.ab.notificationservice.kafka.event.TradeCreatedEvent," +  // ✅ ADD THIS
                        "com.ab.orderservice.kafka.event.OrderCreatedEvent:com.ab.notificationservice.kafka.event.OrderCreatedEvent," +
                        "com.ab.orderservice.kafka.event.OrderCancelledEvent:com.ab.notificationservice.kafka.event.OrderCancelledEvent," +
                        "com.ab.orderservice.kafka.event.OrderReplacedEvent:com.ab.notificationservice.kafka.event.OrderReplacedEvent," +
                        "com.ab.orderservice.kafka.event.OrderPartiallyFilledEvent:com.ab.notificationservice.kafka.event.OrderPartiallyFilledEvent," +
                        "com.ab.orderservice.kafka.event.OrderFilledEvent:com.ab.notificationservice.kafka.event.OrderFilledEvent"
        );
        // Note: explicit deserializers passed here are standard; ErrorHandlingDeserializer is configured via props.
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JacksonJsonDeserializer<>(Object.class, false)
        );
    }

    /**
     * Listener container factory used by @KafkaListener methods.
     * DefaultErrorHandler keeps the container running and logs processing errors.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);

        // log errors instead of silently stopping
        factory.setCommonErrorHandler(new DefaultErrorHandler());

        return factory;
    }
}