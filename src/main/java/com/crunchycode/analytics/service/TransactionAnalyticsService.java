package com.crunchycode.analytics.service;

import com.crunchycode.analytics.model.Transaction;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Common contract for executing the six core transaction analytics queries.
 */
public interface TransactionAnalyticsService {

    /**
     * Query 1: Top N customers by total spend (SUCCESS transactions only).
     */
    List<Map.Entry<String, BigDecimal>> getTopCustomers(List<Transaction> transactions, int limit);

    /**
     * Query 2: Monthly spend totals ordered chronologically (SUCCESS transactions only).
     */
    Map<YearMonth, BigDecimal> getMonthlyTotals(List<Transaction> transactions);

    /**
     * Query 3: Duplicate transactions (records with same customerId, timestamp, and amount).
     */
    List<Transaction> findDuplicates(List<Transaction> transactions);

    /**
     * Query 4: Flagged anomalies (transactions whose amount >= threshold).
     */
    List<Transaction> findAnomalies(List<Transaction> transactions, BigDecimal threshold);

    /**
     * Query 5: Spend breakdown grouped by category (SUCCESS transactions only).
     */
    Map<String, BigDecimal> getCategoryBreakdown(List<Transaction> transactions);

    /**
     * Query 6: Transaction failure rate by merchant (FAILED / TOTAL).
     */
    Map<String, Double> getFailureRateByMerchant(List<Transaction> transactions);
}
