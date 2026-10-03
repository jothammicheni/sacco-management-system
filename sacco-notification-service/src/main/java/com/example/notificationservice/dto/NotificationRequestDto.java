package com.example.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDto {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotNull(message = "Channel is required")
    private Channel channel;

    @NotBlank(message = "Recipient address is required")
    @Size(max = 255)
    private String recipientAddress;

    @NotBlank(message = "Message body is required")
    @Size(max = 1000)
    private String messageBody;

    public enum Channel {
        SMS,
        EMAIL
    }
}