package com.example.saccotransactionservice.service;


import com.example.saccotransactionservice.entity.JournalEntry;
import com.example.saccotransactionservice.entity.LedgerAccount;
import com.example.saccotransactionservice.entity.Transaction;
import com.example.saccotransactionservice.enums.*;
import com.example.saccotransactionservice.repository.LedgerAccountRepository;
import com.example.saccotransactionservice.repository.OutboxEventRepository;
import com.example.saccotransactionservice.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class LedgerServiceImpl {
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

    ){
        //handle indeponcy
      return  transactionRepository.findByTransactionReference(reference)
              .orElseGet(()->executeDeposit(
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
    ){
        if(amount==null || amount.compareTo(BigDecimal.ZERO)<=0){
            throw  new IllegalArgumentException("The deposit amount cannot be zero or less");
        }

        // now get the vault channel

        String glAccountNumber = resolveGlAccount(String.valueOf(channel));
        LedgerAccount glAccount;
        LedgerAccount memberAccount;

        // lock the  active roles
        if(glAccountNumber.compareTo(memberAccountNumber)<0){
            glAccount = lockActiveAccount(glAccountNumber);
            memberAccount = lockActiveAccount(memberAccountNumber);
        }else {
            memberAccount = lockActiveAccount(memberAccountNumber);
            glAccount = lockActiveAccount(glAccountNumber);
        }

        ///compute balances

        BigDecimal newGlAccountBalance= glAccount.getCurrentBalance().add(amount);
        BigDecimal newMemberBalance= memberAccount.getCurrentBalance().add(amount);

        Transaction tx= Transaction.builder()
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

        JournalEntry  debitEntry= JournalEntry.builder()
                .ledgerAccount(glAccount)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .balanceAfter(newGlAccountBalance)
                .build();
        JournalEntry creditEntry= JournalEntry.builder()
                .ledgerAccount(memberAccount)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .balanceAfter(newMemberBalance)
                .build();

        tx.addJournalEntry(debitEntry);
        tx.addJournalEntry(creditEntry);


        //enforce the zero sum math
        assertBalanced(tx.getJournalEntries());

        // porpulate the new balances

        memberAccount.setCurrentBalance(newMemberBalance);
        glAccount.setCurrentBalance(newGlAccountBalance);

        Transaction savedTx =transactionRepository.save(tx);
        return  savedTx;
    }


    private String resolveGlAccount(String channel){
        return switch (channel){
//             case null -> throw  new IllegalArgumentException("channel cant be null"); // works on java 21+
             case "MPESA" -> GL_MPESA_FLOAT;
             case  "CASH" -> GL_CASH_VAULT;
             case "BANK"  ->GL_BANK_MAIN;
             case  "WITHDRAW_REVENUE" ->GL_FEE_REVENUE;
            default -> throw  new IllegalArgumentException("Wrong channel Input");
         };

    }

    private LedgerAccount lockActiveAccount(String accountNumber){
        LedgerAccount account= ledgerAccountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(()-> new ResourceNotFoundException("Account Number not found"));
        if(account.getStatus()!= AccountStatus.ACTIVE){
            throw  new IllegalStateException("Account is not active :" + accountNumber);
        }

        return  account;
    }


    private void assertBalanced(List<JournalEntry> entries) {

        if(entries.size()<2){
            throw new IllegalStateException("Double-entry requires at least 2 journal entries");
        }
        BigDecimal debits= entries.stream()
                .filter(e->e.getEntryType()==EntryType.DEBIT)
                .map(JournalEntry::getAmount)
                .reduce(BigDecimal.ZERO , BigDecimal::add);

        BigDecimal credits= entries.stream()
                .filter(e->e.getEntryType()==EntryType.CREDIT)
                .map(JournalEntry:: getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if(debits.compareTo(credits) !=0){
            throw new IllegalStateException("Unbalanced journal entries! Debits=" + debits + ", Credits=" + credits);
        }
    }


    }
