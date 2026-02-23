package com.ab.orderservice.kafka;

import com.ab.orderservice.kafka.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventsProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        String key = String.valueOf(event.orderId()); // orderId is Long -> key must be String

        kafkaTemplate.send(OrderKafkaTopics.ORDERS_EVENTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka send FAILED. eventId={}, orderId={}",
                                event.eventId(), event.orderId(), ex);
                        return;
                    }
                    if (result == null || result.getRecordMetadata() == null) {
                        log.info("Kafka send success (no metadata). eventId={}", event.eventId());
                        return;
                    }
                    log.info("Kafka send success. topic={}, partition={}, offset={}, eventId={}, orderId={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.eventId(),
                            event.orderId());
                });
    }
}
