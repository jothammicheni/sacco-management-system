package com.example.saccoaccountservice.service;

import com.example.saccoaccountservice.dto.AccountRequestDto;
import com.example.saccoaccountservice.dto.AccountResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    /**
     * Creates the default set of accounts for a newly created member.
     * Called by MemberEventConsumer when it processes a member.created event.
     * Idempotent — safe to call more than once for the same member.
     */
    void createDefaultAccountsFor(UUID memberId);

    /**
     * Manually create an account (e.g. admin creates a HOLIDAY_SAVINGS account).
     * Not used by the consumer flow.
     */
    AccountResponseDto create(AccountRequestDto request);

    /**
     * Look up a single account by its database id.
     */
    AccountResponseDto getById(Long id);

    /**
     * Look up a single account by its human-readable account number.
     */
    AccountResponseDto getByAccountNumber(String accountNumber);

    /**
     * All accounts belonging to a given member.
     */
    List<AccountResponseDto> getByMemberId(UUID memberId);

    /**
     * Paginated accounts for a member — for member-facing list screens.
     */
    Page<AccountResponseDto> getByMemberId(UUID memberId, Pageable pageable);

    /**
     * Paginated list of every account, filtered by status.
     * For admin/reporting use.
     */
    Page<AccountResponseDto> getAllByStatus(String status, Pageable pageable);

    /**
     * Soft-delete an account.
     */
    void softDelete(Long id);

    /**
     * Restore a soft-deleted account.
     */
    void restore(Long id);

    void updateAcountStatusOnMemberDelete(UUID memberId);
}