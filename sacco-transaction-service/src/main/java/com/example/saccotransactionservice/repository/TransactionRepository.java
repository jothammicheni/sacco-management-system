package com.example.saccotransactionservice.repository;

import com.example.saccotransactionservice.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByTransactionReference(String transactionReference);

    Optional<Transaction> findByTransactionReference(String transactionReference);

    // Powers the Recent Transactions feed on your Angular Member Dashboard
    Page<Transaction> findByMemberIdOrderByCreatedAtDesc(String memberId, Pageable pageable);

    Page<Transaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);
}
