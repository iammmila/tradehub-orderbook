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

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeCreatedNotificationsConsumer {

    private final NotificationService service;
    private final SimpMessagingTemplate ws; // optional for later UI

    @KafkaListener(
            topics = "${app.kafka.topics.trade-events}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTradeCreated(TradeCreatedEvent event) {

        String title = "Trade executed: " + event.instrument();
        String message = "Price " + event.price() + ", Qty " + event.quantity() +
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

        // WebSocket push (nice to have, you can ignore UI now)
        ws.convertAndSend("/topic/notifications." + buyerNotif.getUserId(), NotificationMapper.toDto(buyerNotif));
        ws.convertAndSend("/topic/notifications." + sellerNotif.getUserId(), NotificationMapper.toDto(sellerNotif));

        log.info("NOTIFICATION saved trade eventId={} buyerUserId={} sellerUserId={}",
                event.eventId(), event.buyerUserId(), event.sellerUserId());
    }
}
