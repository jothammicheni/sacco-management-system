package com.example.saccoaccountservice.controller;

import com.example.saccoaccountservice.dto.AccountRequestDto;
import com.example.saccoaccountservice.dto.AccountResponseDto;
import com.example.saccoaccountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ==========================================================
    // 1. My accounts (authenticated user — reads X-User-Id)
    // ==========================================================
    @GetMapping("/my-accounts")
    public List<AccountResponseDto> myAccounts(@RequestHeader("X-User-Id") UUID userId) {
        return accountService.getByMemberId(userId);
    }

    // ==========================================================
    // 2. My accounts, paginated
    // ==========================================================
    @GetMapping("/my-accounts/paged")
    public Page<AccountResponseDto> myAccountsPaged(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return accountService.getByMemberId(userId, pageable);
    }

    // ==========================================================
    // 3. Single account by ID (ownership check)
    // ==========================================================
    @GetMapping("/{id}")
    public AccountResponseDto getById(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {

        AccountResponseDto account = accountService.getById(id);

        // CUSTOMER can only view their own accounts; ADMIN can view any
        if (!"ADMIN".equals(role) && !account.getMemberId().equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You can only view your own accounts");
        }
        return account;
    }

    // ==========================================================
    // 4. Account by account number (ownership check)
    // ==========================================================
    @GetMapping("/by-number/{accountNumber}")
    public AccountResponseDto getByAccountNumber(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String accountNumber) {

        AccountResponseDto account = accountService.getByAccountNumber(accountNumber);

        if (!"ADMIN".equals(role) && !account.getMemberId().equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You can only view your own accounts");
        }
        return account;
    }

    // ==========================================================
    // 5. Admin: create account for a member
    // ==========================================================
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponseDto create(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody AccountRequestDto request) {

        if (!"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admin only");
        }
        return accountService.create(request);
    }

    // ==========================================================
    // 6. Admin: list all accounts by status
    // ==========================================================
    @GetMapping("/admin/by-status")
    public Page<AccountResponseDto> getAllByStatus(
            @RequestHeader("X-User-Role") String role,
            @RequestParam String status,
            Pageable pageable) {

        if (!"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admin only");
        }
        return accountService.getAllByStatus(status, pageable);
    }

    // ==========================================================
    // 7. Admin: soft-delete an account
    // ==========================================================
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {

        if (!"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admin only");
        }
        accountService.softDelete(id);
    }

    // ==========================================================
    // 8. Admin: restore a soft-deleted account
    // ==========================================================
    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {

        if (!"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admin only");
        }
        accountService.restore(id);
    }
}