package com.example.saccotransactionservice.service;

import com.example.saccotransactionservice.entity.JournalEntry;
import com.example.saccotransactionservice.entity.LedgerAccount;
import com.example.saccotransactionservice.entity.OutboxEvent;
import com.example.saccotransactionservice.entity.Transaction;
import com.example.saccotransactionservice.enums.*;
import com.example.saccotransactionservice.repository.LedgerAccountRepository;
import com.example.saccotransactionservice.repository.OutboxEventRepository;
import com.example.saccotransactionservice.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
//public class LedgerService {
    public class TestService {

    private static final String GL_MPESA_FLOAT = "GL-1001";
    private static final String GL_BANK_MAIN   = "GL-1002";
    private static final String GL_CASH_VAULT  = "GL-1003";
    private static final String GL_FEE_REVENUE = "GL-4001";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public Transaction postDeposit(
            String reference,
            String memberId,
            String memberAccountNumber,
            PaymentChannel channel,
            BigDecimal amount,
            String description,
            String initiatedBy,
            Map<String, Object> metadata
    ) {
        // 1. Idempotency check (prevents duplicate M-Pesa callbacks or double clicks)
        return transactionRepository.findByTransactionReference(reference)
                .orElseGet(() -> executeDeposit(
                        reference, memberId, memberAccountNumber, channel, amount, description, initiatedBy, metadata
                ));
    }

    private Transaction executeDeposit(
            String reference,
            String memberId,
            String memberAccountNumber,
            PaymentChannel channel,
            BigDecimal amount,
            String description,
            String initiatedBy,
            Map<String, Object> metadata
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        String glAccountNumber = resolveGlAccount(channel);

        // 2. Lock accounts in alphabetical order by accountNumber to prevent MySQL deadlocks
        LedgerAccount glAccount;
        LedgerAccount memberAccount;
        if (glAccountNumber.compareTo(memberAccountNumber) < 0) {
            glAccount = lockActiveAccount(glAccountNumber);
            memberAccount = lockActiveAccount(memberAccountNumber);
        } else {
            memberAccount = lockActiveAccount(memberAccountNumber);
            glAccount = lockActiveAccount(glAccountNumber);
        }

        // 3. Compute new balances
        BigDecimal newGlBalance = glAccount.getCurrentBalance().add(amount);
        BigDecimal newMemberBalance = memberAccount.getCurrentBalance().add(amount);

        // 4. Build Transaction Header
        Transaction tx = Transaction.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .transactionReference(reference)
                .memberId(memberId)
                .accountNumber(memberAccountNumber)
                .transactionType(TransactionType.DEPOSIT)
                .paymentChannel(channel)
                .description(description)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .initiatedBy(initiatedBy != null ? initiatedBy : "SYSTEM")
                .metadata(metadata)
                .build();

        // 5. Build Double-Entry Journal Lines (DEBIT SACCO Asset, CREDIT Member Liability)
        JournalEntry debitEntry = JournalEntry.builder()
                .ledgerAccount(glAccount)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .balanceAfter(newGlBalance)
                .build();

        JournalEntry creditEntry = JournalEntry.builder()
                .ledgerAccount(memberAccount)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .balanceAfter(newMemberBalance)
                .build();

        tx.addJournalEntry(debitEntry);
        tx.addJournalEntry(creditEntry);

        // 6. Enforce zero-sum invariant before saving
        assertBalanced(tx.getJournalEntries());

        // 7. Update running balances & save
        glAccount.setCurrentBalance(newGlBalance);
        memberAccount.setCurrentBalance(newMemberBalance);
        Transaction savedTx = transactionRepository.save(tx);

        // 8. Write Outbox Event for account-service & notification-service
        OutboxEvent outbox = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType("TRANSACTION")
                .aggregateId(savedTx.getTransactionUuid())
                .eventType("DEPOSIT_COMPLETED")
                .topic("ledger.transactions")
                .kafkaKey(memberAccountNumber)
                .payload(Map.of(
                        "eventId", UUID.randomUUID().toString(),
                        "transactionUuid", savedTx.getTransactionUuid(),
                        "transactionReference", savedTx.getTransactionReference(),
                        "memberId", memberId,
                        "accountNumber", memberAccountNumber,
                        "amount", amount,
                        "entryType", "CREDIT",
                        "newBalance", newMemberBalance,
                        "channel", channel.name()
                ))
                .build();

        outboxEventRepository.save(outbox);
        return savedTx;
    }

    private LedgerAccount lockActiveAccount(String accountNumber) {
        LedgerAccount account = ledgerAccountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Ledger account not found: " + accountNumber));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not ACTIVE: " + accountNumber);
        }
        return account;
    }

    private String resolveGlAccount(PaymentChannel channel) {
        return switch (channel) {
            case MPESA -> GL_MPESA_FLOAT;
            case BANK  -> GL_BANK_MAIN;
            case CASH  -> GL_CASH_VAULT;
            default    -> throw new IllegalArgumentException("Unsupported external payment channel: " + channel);
        };
    }

    private void assertBalanced(List<JournalEntry> entries) {
        if (entries.size() < 2) {
            throw new IllegalStateException("Double-entry requires at least 2 journal entries");
        }
        BigDecimal debits = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .map(JournalEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credits = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .map(JournalEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debits.compareTo(credits) != 0) {
            throw new IllegalStateException("Unbalanced journal entries! Debits=" + debits + ", Credits=" + credits);
        }
    }
}
