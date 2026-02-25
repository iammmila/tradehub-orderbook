package com.ab.notificationservice.config;

import com.ab.notificationservice.kafka.event.TradeCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Important: use ErrorHandlingDeserializer wrapping JsonDeserializer
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);

        // JsonDeserializer settings
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        props.put(JacksonJsonDeserializer.TYPE_MAPPINGS,
                "com.ab.tradeservice.kafka.event.TradeCreatedEvent:com.ab.notificationservice.kafka.event.TradeCreatedEvent," +
                        "com.ab.orderservice.kafka.event.TradeCreatedEvent:com.ab.notificationservice.kafka.event.TradeCreatedEvent," +  // ✅ ADD THIS
                        "com.ab.orderservice.kafka.event.OrderCreatedEvent:com.ab.notificationservice.kafka.event.OrderCreatedEvent," +
                        "com.ab.orderservice.kafka.event.OrderCancelledEvent:com.ab.notificationservice.kafka.event.OrderCancelledEvent," +
                        "com.ab.orderservice.kafka.event.OrderReplacedEvent:com.ab.notificationservice.kafka.event.OrderReplacedEvent," +
                        "com.ab.orderservice.kafka.event.OrderPartiallyFilledEvent:com.ab.notificationservice.kafka.event.OrderPartiallyFilledEvent," +
                        "com.ab.orderservice.kafka.event.OrderFilledEvent:com.ab.notificationservice.kafka.event.OrderFilledEvent"
        );

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JacksonJsonDeserializer<>(Object.class, false)
        );
    }

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