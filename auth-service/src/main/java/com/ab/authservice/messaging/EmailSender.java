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
            if (cmd.getTemplate() == null) {
                return SendResult.fail("MISSING_TEMPLATE", "template is required");
            }

            return switch (cmd.getTemplate()) {
                case PASSWORD_RESET -> sendPasswordReset(cmd);
                case EMAIL_VERIFICATION -> sendEmailVerification(cmd);
            };

        } catch (Exception e) {
            return SendResult.fail("EMAIL_SEND_FAILED", e.getMessage());
        }
    }

    private SendResult sendPasswordReset(NotificationCommand cmd) {
        String resetLink = getVar(cmd, "resetLink");
        if (resetLink == null || resetLink.isBlank()) {
            return SendResult.fail("MISSING_VARIABLE", "resetLink is required for PASSWORD_RESET");
        }
        emailService.sendResetPasswordEmail(cmd.getTo(), resetLink);
        return SendResult.ok("smtp");
    }

    private SendResult sendEmailVerification(NotificationCommand cmd) {
        String verifyLink = getVar(cmd, "verifyLink");
        if (verifyLink == null || verifyLink.isBlank()) {
            return SendResult.fail("MISSING_VARIABLE", "verifyLink is required for EMAIL_VERIFICATION");
        }
        emailService.sendEmailVerification(cmd.getTo(), verifyLink);
        return SendResult.ok("smtp");
    }

    private String getVar(NotificationCommand cmd, String key) {
        if (cmd.getVariables() == null) return null;
        Object v = cmd.getVariables().get(key);
        return v == null ? null : v.toString();
    }
}
