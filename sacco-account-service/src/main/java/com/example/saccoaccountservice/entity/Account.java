package com.example.saccoaccountservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_accounts_account_number", columnNames = "account_number"),
                @UniqueConstraint(name = "uq_accounts_member_type",    columnNames = {"member_id", "account_type"})
        },
        indexes = {
                @Index(name = "idx_accounts_member_id",  columnList = "member_id"),
                @Index(name = "idx_accounts_status",     columnList = "status"),
                @Index(name = "idx_accounts_deleted_at", columnList = "deleted_at")
        }
)
@SQLDelete(sql = "UPDATE accounts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    /**
     * Logical reference to sacco_member_db.members.id.
     * NOT a JPA relationship — cross-service references are by id only.
     */
    @Column(name = "member_id", nullable = false, length = 36)
    private UUID memberId;

    @Builder.Default
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ---------- Enums ----------

    public enum AccountType {
        SHARES,
        DEPOSITS,
        HOLIDAY_SAVINGS
    }

    public enum Status {
        ACTIVE,
        FROZEN
    }

    // ---------- Lifecycle hook ----------

    @PrePersist
    void onCreate() {
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (status == null) {
            status = Status.ACTIVE;
        }
    }
}