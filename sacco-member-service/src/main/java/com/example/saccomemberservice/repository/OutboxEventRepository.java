package com.example.saccomemberservice.repository;

import com.example.saccomemberservice.entity.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Scheduler query — batch of pending events, oldest first. */
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();

    /** Same query with a configurable batch size. */
    Page<OutboxEvent> findByPublishedAtIsNullOrderByIdAsc(Pageable pageable);

    /** Check by global event id (used if you ever need to verify an event exists). */
    Optional<OutboxEvent> findByEventId(String eventId);

    /** All events for one aggregate (debugging: "show me all events for this member"). */
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByIdAsc(
            String aggregateType, String aggregateId);

    /** Count pending events, useful for alerting on backlog. */
    long countByPublishedAtIsNull();

    /** Count events stuck with too many failed attempts. */
    long countByPublishedAtIsNullAndAttemptsGreaterThan(int attempts);
}