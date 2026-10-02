package com.example.saccoaccountservice.utils;

import com.example.saccoaccountservice.entity.Account;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;

/**
 * Generates human-readable, unique account numbers.
 *
 * Format: <PREFIX>-<YEAR>-<SUFFIX>
 *   SHARES          → SHR-2026-A1B2C3D4
 *   DEPOSITS        → DEP-2026-E5F6G7H8
 *   HOLIDAY_SAVINGS → HOL-2026-I9J0K1L2
 *
 * Suffix is a random 8-character uppercase hex fragment derived from a UUID.
 * This is unique enough for a SACCO dev environment. For production,
 * replace with a DB sequence per account type.
 */
@Component
public class AccountNumberGenerator {

    public String nextAccountNumber(Account.AccountType type) {
        String prefix = prefixFor(type);
        String suffix = randomSuffix();
        int year = Year.now().getValue();
        return String.format("%s-%d-%s", prefix, year, suffix);
    }

    // ----------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------

    private String prefixFor(Account.AccountType type) {
        return switch (type) {
            case SHARES          -> "SHR";
            case DEPOSITS        -> "DEP";
            case HOLIDAY_SAVINGS -> "HOL";
        };
    }

    private String randomSuffix() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }
}