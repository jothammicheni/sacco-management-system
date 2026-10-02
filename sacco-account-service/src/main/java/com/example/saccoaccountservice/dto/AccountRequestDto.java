package com.example.saccoaccountservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRequestDto {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    public enum AccountType {
        SHARES,
        DEPOSITS,
        HOLIDAY_SAVINGS
    }
}