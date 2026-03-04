package com.ab.authservice.messaging;

// Strategy interface: one implementation per channel (EMAIL, SMS, etc.).
public interface NotificationSender {
    Channel channel();

    SendResult send(NotificationCommand cmd);
}
