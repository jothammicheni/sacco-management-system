package com.example.saccotransactionservice.repository;

import com.example.saccotransactionservice.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findByTransactionId(Long transactionId);

    // Powers member account statements with running balanceAfter
    Page<JournalEntry> findByLedgerAccountAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);

    // Audit check: Must always return 0.00
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN j.entryType = 'DEBIT' THEN j.amount ELSE -j.amount END), 0)
        FROM JournalEntry j
        """)
    BigDecimal calculateGlobalLedgerImbalance();
}
