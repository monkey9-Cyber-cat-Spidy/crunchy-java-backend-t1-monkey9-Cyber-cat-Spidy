package com.crunchycode.analytics.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Represents a single financial transaction record parsed from CSV.
 * Uses immutable fields and standard value-object semantics.
 */
public record Transaction(
        String transactionId,
        String customerId,
        LocalDateTime timestamp,
        BigDecimal amount,
        String category,
        String status,
        String merchant
) {
    public Transaction {
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        Objects.requireNonNull(customerId, "customerId cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(merchant, "merchant cannot be null");
    }

    /**
     * Helper to extract YearMonth for monthly aggregation queries.
     */
    public YearMonth yearMonth() {
        return YearMonth.from(timestamp);
    }

    /**
     * Helper signature used to detect duplicate transactions.
     * Often duplicates in transaction datasets share identical customer, timestamp, and amount.
     */
    public String duplicateKey() {
        return customerId + "|" + timestamp.toString() + "|" + amount.toPlainString();
    }
}
