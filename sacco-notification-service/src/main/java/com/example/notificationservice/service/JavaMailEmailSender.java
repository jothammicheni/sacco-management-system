package com.example.notificationservice.service;

import com.example.notificationservice.dto.SendEmailResultDto;
import com.example.notificationservice.service.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    public SendEmailResultDto send(String to, String subject, String body) {

        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be null or blank");
        }
        if (subject == null) {
            subject = "";
        }
        if (body == null) {
            body = "";
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);   // false = plain text, not HTML

            mailSender.send(message);

            // JavaMail doesn't return a message id — we use a local reference
            String reference = "javamail-" + System.currentTimeMillis();
            log.info("Email sent to {} (ref={})", to, reference);

            return SendEmailResultDto.success(reference);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            return SendEmailResultDto.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage(), e);
            return SendEmailResultDto.failure("Unexpected error: " + e.getMessage());
        }
    }
}