package com.example.saccoaccountservice.service;
import com.example.saccoaccountservice.dto.AccountRequestDto;
import com.example.saccoaccountservice.dto.AccountResponseDto;
import com.example.saccoaccountservice.entity.Account;
import com.example.saccoaccountservice.kafka.events.AccountOutboxWriter;
import com.example.saccoaccountservice.repository.AccountRepository;
import com.example.saccoaccountservice.service.AccountService;
import com.example.saccoaccountservice.utils.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepo;
    private final AccountOutboxWriter outboxWriter;
    private final com.example.saccoaccountservice.utils.AccountNumberGenerator numberGenerator;

    // ----------------------------------------------------------
    // 1. Called by MemberEventConsumer for each member.created event.
    //    Idempotent: if the SHARES account already exists, does nothing.
    // ----------------------------------------------------------
    @Override
    @Transactional
    public void createDefaultAccountsFor(UUID memberId) {

        boolean alreadyExists = accountRepo.existsByMemberIdAndAccountType(
                memberId, Account.AccountType.SHARES);

        if (alreadyExists) {
            log.info("SHARES account already exists for member {}, skipping", memberId);
            return;
        }

        Account shares = Account.builder()
                .accountNumber(numberGenerator.nextAccountNumber(Account.AccountType.SHARES))
                .memberId(memberId)
                .balance(BigDecimal.ZERO)
                .accountType(Account.AccountType.SHARES)
                .status(Account.Status.ACTIVE)
                .build();

        Account saved = accountRepo.save(shares);

        outboxWriter.writeAccountCreated(saved);

        log.info("Created SHARES account {} for member {}",
                saved.getAccountNumber(), memberId);
    }

    // ----------------------------------------------------------
    // 2. Manual account creation (admin-only, not used by the consumer)
    // ----------------------------------------------------------
    @Override
    @Transactional
    public AccountResponseDto create(AccountRequestDto request) {

        Account.AccountType type = Account.AccountType.valueOf(request.getAccountType().name());

        boolean alreadyExists = accountRepo.existsByMemberIdAndAccountType(
                request.getMemberId(), type);

        if (alreadyExists) {
            throw new IllegalStateException(
                    "Member already has a " + type + " account");
        }

        Account account = Account.builder()
                .accountNumber(numberGenerator.nextAccountNumber(type))
                .memberId(request.getMemberId())
                .balance(BigDecimal.ZERO)
                .accountType(type)
                .status(Account.Status.ACTIVE)
                .build();

        Account saved = accountRepo.save(account);
        outboxWriter.writeAccountCreated(saved);

        return AccountResponseDto.fromEntity(saved);
    }

    // ----------------------------------------------------------
    // 3. Read operations
    // ----------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getById(Long id) {
        Account account = accountRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + id));
        return AccountResponseDto.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getByAccountNumber(String accountNumber) {
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NoSuchElementException(
                        "Account not found: " + accountNumber));
        return AccountResponseDto.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getByMemberId(UUID memberId) {
        return accountRepo.findByMemberId(memberId).stream()
                .map(AccountResponseDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponseDto> getByMemberId(UUID memberId, Pageable pageable) {
        return accountRepo.findByMemberId(memberId, pageable)
                .map(AccountResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponseDto> getAllByStatus(String status, Pageable pageable) {
        Account.Status s = Account.Status.valueOf(status.toUpperCase());
        return accountRepo.findByStatus(s, pageable)
                .map(AccountResponseDto::fromEntity);
    }

    // ----------------------------------------------------------
    // 4. Soft delete / restore
    // ----------------------------------------------------------
    @Override
    @Transactional
    public void softDelete(Long id) {
        Account account = accountRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + id));
        accountRepo.delete(account);   // triggers @SQLDelete → UPDATE accounts SET deleted_at
    }

    @Override
    @Transactional
    public void restore(Long id) {
        int rows = accountRepo.restoreById(id);
        if (rows == 0) {
            throw new NoSuchElementException("Account not found or not deleted: " + id);
        }
    }

    @Override
    @Transactional
    public void updateAcountStatusOnMemberDelete(UUID memberId) {
       List<Account> accounts=accountRepo.findByMemberId(memberId);
       if(accounts.isEmpty()){
           log.info("No accounts found for member {}", memberId);
           return;
       }
       for (Account account:accounts){
           if(account.getStatus()== Account.Status.FROZEN){
               log.info("Account {} already frozen", account.getAccountNumber());
               continue;
           }

           account.setStatus(Account.Status.FROZEN);
           Account saved= accountRepo.save(account);
           outboxWriter.writeAccountFrozen(saved);

       }
    }
}