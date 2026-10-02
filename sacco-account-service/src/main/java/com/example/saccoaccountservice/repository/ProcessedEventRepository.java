package com.example.saccoaccountservice.repository;

import com.example.saccoaccountservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, ProcessedEvent.ProcessedEventId> {

    /** Have we already processed this event for this consumer? */
    boolean existsByEventIdAndConsumer(String eventId, String consumer);

    /** All events processed by a consumer, newest first — for debugging. */
    List<ProcessedEvent> findByConsumerOrderByProcessedAtDesc(String consumer);

    /** Cleanup: find rows older than a threshold. */
    List<ProcessedEvent> findByProcessedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM ProcessedEvent p WHERE p.processedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}