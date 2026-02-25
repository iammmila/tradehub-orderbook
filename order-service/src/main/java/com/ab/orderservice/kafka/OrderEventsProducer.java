package com.ab.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventsProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String key, Object event) {
        kafkaTemplate.send(OrderKafkaTopics.ORDERS_EVENTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("KAFKA SENT FAILED. key={}, eventClass={}", key, event.getClass().getSimpleName(), ex);
                        return;
                    }
                    if (result == null || result.getRecordMetadata() == null) {
                        log.info("KAFKA SENT success (no metadata). key={}, eventClass={}", key, event.getClass().getSimpleName());
                        return;
                    }
                    log.info("KAFKA SENT success. topic={}, partition={}, offset={}, key={}, eventClass={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key,
                            event.getClass().getSimpleName());
                });
    }
}
