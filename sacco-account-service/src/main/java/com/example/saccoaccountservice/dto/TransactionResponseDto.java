package com.example.saccoaccountservice.dto;

import com.example.saccoaccountservice.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDto {

    private Long id;
    private Long accountId;
    private String transactionReference;
    private BigDecimal amount;
    private Transaction.TransactionType transactionType;
    private LocalDateTime createdAt;

    public static TransactionResponseDto fromEntity(Transaction t) {
        return TransactionResponseDto.builder()
                .id(t.getId())
                .accountId(t.getAccountId())
                .transactionReference(t.getTransactionReference())
                .amount(t.getAmount())
                .transactionType(t.getTransactionType())
                .createdAt(t.getCreatedAt())
                .build();
    }
}