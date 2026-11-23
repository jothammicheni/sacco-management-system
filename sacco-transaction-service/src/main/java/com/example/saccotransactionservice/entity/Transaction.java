package com.example.saccotransactionservice.entity;


import com.example.saccotransactionservice.enums.PaymentChannel;
import com.example.saccotransactionservice.enums.TransactionStatus;
import com.example.saccotransactionservice.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "transaction_uuid", nullable = false, unique = true, updatable = false, length = 36)
    private String transactionUuid = UUID.randomUUID().toString();

    @Column(name = "transaction_reference", nullable = false, unique = true, updatable = false, length = 100)
    private String transactionReference;

    @Column(name = "member_id", length = 36)
    private String memberId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_channel", nullable = false, length = 20)
    private PaymentChannel paymentChannel;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Builder.Default
    @Column(name = "initiated_by", nullable = false, length = 64)
    private String initiatedBy = "SYSTEM";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata;

    @Builder.Default
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<JournalEntry> journalEntries = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void addJournalEntry(JournalEntry entry) {
        journalEntries.add(entry);
        entry.setTransaction(this);
    }
}
