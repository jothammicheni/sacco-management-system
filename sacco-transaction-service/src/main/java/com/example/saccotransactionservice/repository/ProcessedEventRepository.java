package com.example.saccotransactionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<com.sacco.transaction.domain.entity.ProcessedEvent, String> {
}
