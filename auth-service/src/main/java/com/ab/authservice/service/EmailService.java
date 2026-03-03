package com.ab.authservice.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String from;

    @Async
    public void sendResetPasswordEmail(String to, String resetLink) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("resetLink", resetLink);

            String html = templateEngine.process("reset-password", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    msg,
                    MimeMessageHelper.MULTIPART_MODE_MIXED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Reset your password");
            helper.setText(html, true);

            mailSender.send(msg);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send reset email", ex);
        }
    }
}
