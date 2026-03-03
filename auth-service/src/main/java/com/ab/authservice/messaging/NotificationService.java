package com.ab.authservice.messaging;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final List<NotificationSender> senders;

    private Map<Channel, NotificationSender> registry;

    @PostConstruct
    void init() {
        registry = new EnumMap<>(Channel.class);
        for (var s : senders) {
            registry.put(s.channel(), s);
        }
    }

    public SendResult send(NotificationCommand cmd) {
        var sender = registry.get(cmd.getChannel());
        if (sender == null) {
            return SendResult.fail("CHANNEL_NOT_SUPPORTED", "No sender registered for " + cmd.getChannel());
        }
        return sender.send(cmd);
    }
}
