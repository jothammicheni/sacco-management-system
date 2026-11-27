package com.example.saccotransactionservice.repository;

import com.example.saccotransactionservice.entity.LedgerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    Optional<LedgerAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * accounts.id from sacco_account_db. Used by AccountLifecycleConsumer to
     * locate the mirror when a lifecycle event (account.frozen, account.closed)
     * arrives — those events carry only accountId, not accountNumber.
     */
    Optional<LedgerAccount> findBySourceAccountId(Long sourceAccountId);

    // Acquires a MySQL row lock (SELECT ... FOR UPDATE) to prevent concurrent balance corruption
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT la FROM LedgerAccount la WHERE la.accountNumber = :accountNumber")
    Optional<LedgerAccount> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    // Same lock, but keyed on sourceAccountId — used by the freeze handler
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT la FROM LedgerAccount la WHERE la.sourceAccountId = :sourceAccountId")
    Optional<LedgerAccount> findBySourceAccountIdForUpdate(@Param("sourceAccountId") Long sourceAccountId);
}