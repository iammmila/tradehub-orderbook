package com.ab.orderservice.kafka;

/**
 * Central place for Kafka topic names used by order events.
 * Avoids duplicated strings across producers/consumers.
 */
public final class OrderKafkaTopics {
    private OrderKafkaTopics() {}

    public static final String ORDERS_EVENTS = "orders.events";
}