package com.example.saccotransactionservice.repository;

import com.example.saccotransactionservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository   extends JpaRepository<ProcessedEvent, ProcessedEvent.ProcessedEventId> {
    boolean existsByEventIdAndConsumer(String eventId, String consumerName);
}
