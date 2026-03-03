package com.ab.authservice.messaging;

public interface NotificationSender {
    Channel channel();
    SendResult send(NotificationCommand cmd);
}
