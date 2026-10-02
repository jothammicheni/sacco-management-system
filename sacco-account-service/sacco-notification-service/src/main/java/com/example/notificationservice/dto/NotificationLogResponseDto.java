package com.example.notificationservice.dto;

import com.example.notificationservice.entity.NotificationLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogResponseDto {

    private Long id;
    private UUID memberId;
    private NotificationLog.Channel channel;
    private String recipientAddress;
    private String messageBody;
    private NotificationLog.Status status;
    private String kafkaEventId;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;

    public static NotificationLogResponseDto fromEntity(NotificationLog log) {
        return NotificationLogResponseDto.builder()
                .id(log.getId())
                .memberId(log.getMemberId())
                .channel(log.getChannel())
                .recipientAddress(log.getRecipientAddress())
                .messageBody(log.getMessageBody())
                .status(log.getStatus())
                .kafkaEventId(log.getKafkaEventId())
                .sentAt(log.getSentAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}