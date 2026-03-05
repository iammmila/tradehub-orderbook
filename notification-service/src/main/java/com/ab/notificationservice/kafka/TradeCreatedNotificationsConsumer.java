package com.ab.notificationservice.kafka;

import com.ab.notificationservice.kafka.event.TradeCreatedEvent;
import com.ab.notificationservice.mapper.NotificationMapper;
import com.ab.notificationservice.model.Notification;
import com.ab.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Usage:
 * - Consumes trade events from Kafka and creates notifications for both trade parties.
 * - Persists notifications and optionally pushes them to connected WebSocket users.
 * - Keeps trade-specific notification formatting isolated from controller/service layers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeCreatedNotificationsConsumer {

    private final NotificationService service;
    private final SimpMessagingTemplate ws;

    /**
     * Kafka entrypoint for trade events.
     * Uses a dedicated container factory to support JSON event polymorphism and error handling.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.trade-events}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTradeCreated(TradeCreatedEvent event) {
        // Human-readable summary used by UI; keeps message construction consistent across channels (DB + WS).
        String title = "Trade executed: " + event.instrument();
        String message = "Price " + event.price() + ", Quantity " + event.quantity() +
                " (buyOrderId=" + event.buyOrderId() + ", sellOrderId=" + event.sellOrderId() + ")";

        // Buyer notification
        var buyerNotif = service.create(Notification.builder()
                .userId(event.buyerUserId())
                .type("TRADE_EXECUTED")
                .title(title)
                .message(message)
                .entityType("TRADE")
                .entityId(event.eventId())
                .build());

        // Seller notification
        var sellerNotif = service.create(Notification.builder()
                .userId(event.sellerUserId())
                .type("TRADE_EXECUTED")
                .title(title)
                .message(message)
                .entityType("TRADE")
                .entityId(event.eventId())
                .build());

        // WebSocket push for real-time UI; DB persistence remains the source of truth.
        ws.convertAndSendToUser(
                buyerNotif.getUserId().toString(),
                "/queue/notifications",
                NotificationMapper.toDto(buyerNotif));
        ws.convertAndSendToUser(
                sellerNotif.getUserId().toString(),
                "/queue/notifications",
                NotificationMapper.toDto(sellerNotif));
        // Log contains event + both users to support traceability across services.
        log.info("NOTIFICATION saved trade eventId={} buyerUserId={} sellerUserId={}",
                event.eventId(), event.buyerUserId(), event.sellerUserId());
    }
}
