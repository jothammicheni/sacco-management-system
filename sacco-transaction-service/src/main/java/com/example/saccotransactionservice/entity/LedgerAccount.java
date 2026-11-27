package com.example.saccotransactionservice.entity;

import com.example.saccotransactionservice.enums.AccountCategory;
import com.example.saccotransactionservice.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ledger_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ledger_accounts_number", columnNames = "account_number"),
                @UniqueConstraint(name = "uq_ledger_accounts_source_id", columnNames = "source_account_id")
        },
        indexes = {
                @Index(name = "idx_ledger_accounts_member_id", columnList = "member_id"),
                @Index(name = "idx_ledger_accounts_category", columnList = "account_category"),
                @Index(name = "idx_ledger_accounts_deleted_at", columnList = "deleted_at")
        }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * accounts.id from sacco_account_db. NULL for internal SACCO GL accounts
     * (GL-1001 etc). Set for mirrored member accounts so lifecycle events
     * (account.frozen, account.closed) can be applied by accountId.
     */
    @Column(name = "source_account_id", unique = true)
    private Long sourceAccountId;

    @Column(name = "account_number", nullable = false, unique = true, length = 50)
    private String accountNumber;

    @Column(name = "member_id", length = 36, columnDefinition = "varchar(36)")
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_category", nullable = false, length = 20)
    private AccountCategory accountCategory;

    @Column(name = "account_name", nullable = false, length = 120)
    private String accountName;

    @Builder.Default
    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}