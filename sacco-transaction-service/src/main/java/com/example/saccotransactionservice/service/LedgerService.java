package com.example.saccotransactionservice.service;

import com.example.saccotransactionservice.entity.LedgerAccount;
import com.example.saccotransactionservice.entity.Transaction;
import com.example.saccotransactionservice.enums.PaymentChannel;

import java.math.BigDecimal;
import java.util.Map;

public interface LedgerService {

    public Transaction postDeposit(
            String reference,
            String memberId,
            String memberAccountNumber,
            PaymentChannel channel,
            BigDecimal amount,
            String description,
            String initiatedBy,
            Map<String, Object> metadata
    );

    public Transaction executeDeposit(
            String reference,
            String memberId,
            String memberAccountNumber,
            PaymentChannel channel,
            BigDecimal amount,
            String description,
            String initiatedBy,
            Map<String, Object> metadata
    );

    public LedgerAccount lockActiveAccount(String accountNumber);
}
