package com.example.saccoaccountservice.repository;
import com.example.saccoaccountservice.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    Page<Transaction> findByAccountIdAndTransactionType(
            Long accountId, Transaction.TransactionType type, Pageable pageable);

    Page<Transaction> findByAccountIdAndCreatedAtBetween(
            Long accountId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // ---------- Uniqueness ----------

    boolean existsByTransactionReference(String transactionReference);

    // ---------- Aggregations (for balance verification / reporting) ----------

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.accountId = :accountId
              AND t.transactionType = 'DEPOSIT'
            """)
    BigDecimal sumDepositsForAccount(@Param("accountId") Long accountId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.accountId = :accountId
              AND t.transactionType = 'WITHDRAWAL'
            """)
    BigDecimal sumWithdrawalsForAccount(@Param("accountId") Long accountId);
}