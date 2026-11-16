package com.example.notificationservice.service;

import com.example.notificationservice.dto.SendEmailResultDto;

public interface EmailSender {

    /**
     * Sends an email synchronously.
     * Returns a result DTO — never throws for delivery failures (network, provider down, etc.).
     * Throws only for programming errors (e.g., null inputs).
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param body    plain text body (HTML version can be added later)
     * @return result containing success/failure + provider reference or error message
     */
    SendEmailResultDto send(String to, String subject, String body);
}