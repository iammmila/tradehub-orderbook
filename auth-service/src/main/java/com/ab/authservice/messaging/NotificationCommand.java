package com.ab.authservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCommand {
    private Channel channel;
    private String to;
    private Template template;
    private String subject;
    private Map<String, Object> variables;  // { "name": "...", "link": "..." }
    private String locale;
}
