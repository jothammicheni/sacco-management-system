
package com.example.saccoevents.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionCreatedEvent(

        String transactionReference,
        String accountNumber,
        String memberId,
        BigDecimal amount,
        List<JournalEntryEvent> journalEntries,
        Instant occurredAt

) {

    public record JournalEntryEvent(
            String accountNumber,
            String entryType,
            BigDecimal amount
    ) {
    }
}
