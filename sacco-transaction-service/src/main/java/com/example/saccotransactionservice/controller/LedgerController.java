package com.example.saccotransactionservice.controller;

import com.example.saccotransactionservice.entity.Transaction;
import com.example.saccotransactionservice.enums.PaymentChannel;
import com.example.saccotransactionservice.service.LedgerServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerServiceImpl ledgerService;

    /**
     * POST /api/v1/ledger/deposits
     * Idempotent on `reference` — re-posting the same reference returns the
     * existing transaction instead of creating a duplicate.
     */
    @PostMapping("/deposits")
    public ResponseEntity<DepositResponse> postDeposit(
            @Valid @RequestBody DepositRequest request) {

        Transaction tx = ledgerService.postDeposit(
                request.reference(),
                request.memberId(),
                request.memberAccountNumber(),
                request.channel(),
                request.amount(),
                request.description(),
                request.initiatedBy(),
                request.metadata()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DepositResponse.from(tx));
    }

    // ---------- Request / Response DTOs ----------

    public record DepositRequest(
            @NotBlank @Size(max = 100) String reference,
            @NotBlank @Size(max = 36)  String memberId,
            @NotBlank @Size(max = 50)  String memberAccountNumber,
            @NotNull PaymentChannel channel,
            @NotNull @DecimalMin(value = "0.01", message = "amount must be > 0")
            BigDecimal amount,
            @NotBlank @Size(max = 255) String description,
            @Size(max = 64) String initiatedBy,
            Map<String, Object> metadata
    ) {}

    public record DepositResponse(
            Long id,
            String transactionUuid,
            String transactionReference,
            String accountNumber,
            BigDecimal amount,
            String status,
            String paymentChannel
    ) {
        public static DepositResponse from(Transaction tx) {
            return new DepositResponse(
                    tx.getId(),
                    tx.getTransactionUuid(),
                    tx.getTransactionReference(),
                    tx.getAccountNumber(),
                    tx.getAmount(),
                    tx.getStatus().name(),
                    tx.getPaymentChannel().name()
            );
        }
    }
}