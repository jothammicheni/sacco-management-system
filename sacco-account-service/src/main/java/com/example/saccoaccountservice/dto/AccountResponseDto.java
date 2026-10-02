package com.example.saccoaccountservice.dto;

import com.example.saccoaccountservice.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponseDto {

    private Long id;
    private String accountNumber;
    private UUID memberId;
    private BigDecimal balance;
    private Account.AccountType accountType;
    private Account.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AccountResponseDto fromEntity(Account a) {
        return AccountResponseDto.builder()
                .id(a.getId())
                .accountNumber(a.getAccountNumber())
                .memberId(a.getMemberId())
                .balance(a.getBalance())
                .accountType(a.getAccountType())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}