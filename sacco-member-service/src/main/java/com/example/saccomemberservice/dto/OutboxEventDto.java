package com.example.saccomemberservice.dto;

import com.example.saccomemberservice.entity.OutboxEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventDto {

    private Long id;
    private String eventId;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String topic;
    private String kafkaKey;
    private String payload;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private Integer attempts;
    private String lastError;

    public static OutboxEventDto fromEntity(OutboxEvent e) {
        return OutboxEventDto.builder()
                .id(e.getId())
                .eventId(e.getEventId())
                .aggregateType(e.getAggregateType())
                .aggregateId(e.getAggregateId())
                .eventType(e.getEventType())
                .topic(e.getTopic())
                .kafkaKey(e.getKafkaKey())
                .payload(e.getPayload())
                .createdAt(e.getCreatedAt())
                .publishedAt(e.getPublishedAt())
                .attempts(e.getAttempts())
                .lastError(e.getLastError())
                .build();
    }
}