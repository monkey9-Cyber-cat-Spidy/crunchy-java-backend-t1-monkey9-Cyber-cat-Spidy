package com.crunchycode.analytics.service;

import com.crunchycode.analytics.model.Transaction;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Concrete implementations of the 6 analytical queries using Java 21 Streams API
 * and Collectors framework.
 *
 * Written with clean, humanized, declarative idiomatic Java.
 */
public class StreamTransactionService implements TransactionAnalyticsService {

    /**
     * Query 1: Top N Customers by total transaction spend.
     * Collection choice:
     * - Collectors.groupingBy with HashMap under the hood for O(1) aggregation.
     * - Collectors.reducing with BigDecimal::add.
     * - Stream.sorted + limit to extract top N into List<Map.Entry>.
     */
    @Override
    public List<Map.Entry<String, BigDecimal>> getTopCustomers(List<Transaction> transactions, int limit) {
        return transactions.stream()
                .filter(tx -> "SUCCESS".equalsIgnoreCase(tx.status()))
                .collect(Collectors.groupingBy(
                        Transaction::customerId,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Query 2: Monthly Total Spend across all successful transactions.
     * Collection choice:
     * - Collectors.groupingBy with supplier TreeMap::new ensures the resulting map
     *   is naturally sorted by YearMonth chronologically.
     */
    @Override
    public Map<YearMonth, BigDecimal> getMonthlyTotals(List<Transaction> transactions) {
        return transactions.stream()
                .filter(tx -> "SUCCESS".equalsIgnoreCase(tx.status()))
                .collect(Collectors.groupingBy(
                        Transaction::yearMonth,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)
                ));
    }

    /**
     * Query 3: Duplicate Transactions.
     * Grouping by duplicateKey and filtering buckets where size > 1.
     * Collection choice:
     * - HashMap under groupingBy to bucket matching transactions.
     * - FlatMap to extract all occurrences past the first copy into a List<Transaction>.
     */
    @Override
    public List<Transaction> findDuplicates(List<Transaction> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(Transaction::duplicateKey))
                .values()
                .stream()
                .filter(group -> group.size() > 1)
                .flatMap(group -> group.stream().skip(1))
                .collect(Collectors.toList());
    }

    /**
     * Query 4: Anomaly Detection.
     * Filter transactions where amount >= threshold.
     * Collection choice:
     * - ArrayList via Collectors.toList() for sequential elements passing predicate.
     */
    @Override
    public List<Transaction> findAnomalies(List<Transaction> transactions, BigDecimal threshold) {
        return transactions.stream()
                .filter(tx -> tx.amount().compareTo(threshold) >= 0)
                .collect(Collectors.toList());
    }

    /**
     * Query 5: Spend Breakdown by Category.
     * Collection choice:
     * - Collectors.groupingBy produces HashMap<String, BigDecimal> with summed amounts.
     */
    @Override
    public Map<String, BigDecimal> getCategoryBreakdown(List<Transaction> transactions) {
        return transactions.stream()
                .filter(tx -> "SUCCESS".equalsIgnoreCase(tx.status()))
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)
                ));
    }

    /**
     * Query 6: Failure Rate by Merchant.
     * Computes the ratio of FAILED transactions per merchant.
     * Collection choice:
     * - Collectors.groupingBy with custom reducing or custom collector.
     *   Here using a concise groupingBy with an int accumulator [failed, total]
     *   or collectingAndThen for clean functional composition.
     */
    @Override
    public Map<String, Double> getFailureRateByMerchant(List<Transaction> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::merchant,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    long failedCount = list.stream().filter(tx -> "FAILED".equalsIgnoreCase(tx.status())).count();
                                    return list.isEmpty() ? 0.0 : (double) failedCount / list.size();
                                }
                        )
                ));
    }
}
