package com.example.saccotransactionservice.repository;

import com.example.saccotransactionservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByPublishedAtIsNullOrderByIdAsc();

    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();

}
