package com.example.saccoaccountservice.repository;

import com.example.saccoaccountservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Main scheduler query — oldest pending events first. */
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();

    Optional<OutboxEvent> findByEventId(String eventId);

    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByIdAsc(
            String aggregateType, String aggregateId);

    long countByPublishedAtIsNull();

    long countByPublishedAtIsNullAndAttemptsGreaterThan(int attempts);
}