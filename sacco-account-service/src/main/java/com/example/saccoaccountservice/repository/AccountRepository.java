package com.example.saccoaccountservice.repository;
import com.example.saccoaccountservice.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByMemberId(UUID memberId);

    List<Account> findByMemberIdAndStatus(UUID memberId, Account.Status status);

    Optional<Account> findByMemberIdAndAccountType(UUID memberId, Account.AccountType accountType);

    // ---------- Uniqueness checks ----------

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByMemberIdAndAccountType(UUID memberId, Account.AccountType accountType);

    // ---------- Pagination ----------

    Page<Account> findByMemberId(UUID memberId, Pageable pageable);

    Page<Account> findByStatus(Account.Status status, Pageable pageable);

    // ---------- Counts (for account-number generation and monitoring) ----------

    long countByMemberId(UUID memberId);

    @Modifying
    @Query(value = "UPDATE members SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    int restoreById(@Param("id") Long id);
}