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

    // Acquires a MySQL row lock (SELECT ... FOR UPDATE) to prevent concurrent balance corruption
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT la FROM LedgerAccount la WHERE la.accountNumber = :accountNumber")
    Optional<LedgerAccount> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
