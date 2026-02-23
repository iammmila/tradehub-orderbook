package com.ab.orderservice.kafka;

import com.ab.orderservice.kafka.event.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventsProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.trade-created}")
    private String topic;

    public void publish(String key, TradeCreatedEvent event) {
        log.info("Published TradeCreatedEvent to {} key={} eventId={}", topic, key, event.eventId());
        kafkaTemplate.send(topic, key, event);
    }
}
