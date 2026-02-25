package com.ab.notificationservice.kafka;

import com.ab.notificationservice.kafka.event.*;
import com.ab.notificationservice.mapper.NotificationMapper;
import com.ab.notificationservice.model.Notification;
import com.ab.notificationservice.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        topics = "${app.kafka.topics.order-events}",
        containerFactory = "kafkaListenerContainerFactory"
)
public class OrderNotificationsConsumer {

    private final NotificationService service;

    @Nullable
    private final SimpMessagingTemplate ws; // optional

    // ORDER_CREATED
    @KafkaHandler
    public void onCreated(OrderCreatedEvent e) {
        String title = "Order created: " + e.instrument();
        String message = "Side " + e.side()
                + ", qty " + e.quantity()
                + ", price " + e.price()
                + " (orderId=" + e.orderId() + ")";

        saveAndPush(e.userId(), "ORDER_CREATED", title, message, String.valueOf(e.eventId()));
        log.info("NOTIFICATION saved ORDER_CREATED eventId={} orderId={} userId={}", e.eventId(), e.orderId(), e.userId());
    }

    // ORDER_CANCELLED
    @KafkaHandler
    public void onCancelled(OrderCancelledEvent e) {
        String title = "Order cancelled: " + e.instrument();
        String message = "Side " + e.side()
                + ", reason " + (e.reason() == null ? "" : e.reason())
                + " (orderId=" + e.orderId() + ")";

        saveAndPush(e.userId(), "ORDER_CANCELLED", title, message, String.valueOf(e.eventId()));
        log.info("NOTIFICATION saved ORDER_CANCELLED eventId={} orderId={} userId={}", e.eventId(), e.orderId(), e.userId());
    }

    // -ORDER_REPLACED
    @KafkaHandler
    public void onReplaced(OrderReplacedEvent e) {
        String title = "Order updated: " + e.instrument();
        String message = "Side " + e.side()
                + ", price " + e.oldPrice() + " → " + e.newPrice()
                + ", qty " + e.oldQuantity() + " → " + e.newQuantity()
                + " (orderId=" + e.orderId() + ")";

        saveAndPush(e.userId(), "ORDER_REPLACED", title, message, String.valueOf(e.eventId()));
        log.info("NOTIFICATION saved ORDER_REPLACED eventId={} orderId={} userId={}", e.eventId(), e.orderId(), e.userId());
    }

    // ORDER_PARTIALLY_FILLED
    @KafkaHandler
    public void onPartiallyFilled(OrderPartiallyFilledEvent e) {
        // (recommended) this is important, keep it
        String title = "Order partially filled: " + e.instrument();
        String message = "Side " + e.side()
                + ", filled " + e.filledQuantity()
                + ", remaining " + e.remainingQuantity()
                + ", price " + e.price()
                + " (orderId=" + e.orderId() + ")";

        saveAndPush(e.userId(), "ORDER_PARTIALLY_FILLED", title, message, String.valueOf(e.eventId()));
        log.info("NOTIFICATION saved ORDER_PARTIALLY_FILLED eventId={} orderId={} userId={}", e.eventId(), e.orderId(), e.userId());
    }

    //  ORDER_FILLED
    @KafkaHandler
    public void onFilled(OrderFilledEvent e) {
        String title = "Order filled: " + e.instrument();
        String message = "Side " + e.side()
                + ", filled " + e.filledQuantityTotal()
                + ", price " + e.price()
                + " (orderId=" + e.orderId() + ")";

        saveAndPush(e.userId(), "ORDER_FILLED", title, message, String.valueOf(e.eventId()));
        log.info("NOTIFICATION saved ORDER_FILLED eventId={} orderId={} userId={}", e.eventId(), e.orderId(), e.userId());
    }

    @Transactional
    private void saveAndPush(Long userId, String type, String title, String message, String entityId) {
        Notification notif = service.create(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .entityType("ORDER")
                .entityId(entityId)
                .build());

        if (ws != null) {
            ws.convertAndSend("/topic/notifications." + notif.getUserId(), NotificationMapper.toDto(notif));
        }
    }
}
