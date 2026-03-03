package com.ab.authservice.messaging;

import com.ab.authservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSender implements NotificationSender {

    private final EmailService emailService;

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public SendResult send(NotificationCommand cmd) {
        try {
            // 1) extract resetLink from variables
            Object linkObj = (cmd.getVariables() != null) ? cmd.getVariables().get("resetLink") : null;
            String resetLink = linkObj != null ? linkObj.toString() : null;

            if (resetLink == null || resetLink.isBlank()) {
                return SendResult.fail("MISSING_VARIABLE", "resetLink is required for PASSWORD_RESET");
            }

            // 2) call your existing working method
            emailService.sendResetPasswordEmail(cmd.getTo(), resetLink);

            return SendResult.ok("smtp");
        } catch (Exception e) {
            return SendResult.fail("EMAIL_SEND_FAILED", e.getMessage());
        }
    }
}
