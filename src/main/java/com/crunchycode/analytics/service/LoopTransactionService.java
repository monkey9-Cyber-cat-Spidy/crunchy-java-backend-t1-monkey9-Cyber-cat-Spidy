package com.crunchycode.analytics.service;

import com.crunchycode.analytics.model.Transaction;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

/**
 * Concrete implementations of the 6 analytical queries using traditional imperative loops
 * and standard Java Collections (List, HashMap, TreeMap, HashSet).
 *
 * Written with clean, humanized, readable idiomatic Java.
 */
public class LoopTransactionService implements TransactionAnalyticsService {

    /**
     * Query 1: Top N Customers by total transaction spend.
     * Collection choice:
     * - HashMap<String, BigDecimal>: O(1) average lookup/update to aggregate spend per customer.
     * - ArrayList<Map.Entry>: standard list to sort top customers by value descending.
     */
    @Override
    public List<Map.Entry<String, BigDecimal>> getTopCustomers(List<Transaction> transactions, int limit) {
        Map<String, BigDecimal> spendByCustomer = new HashMap<>();

        for (Transaction tx : transactions) {
            // Only count successful transactions for spending calculations
            if ("SUCCESS".equalsIgnoreCase(tx.status())) {
                BigDecimal current = spendByCustomer.get(tx.customerId());
                if (current == null) {
                    spendByCustomer.put(tx.customerId(), tx.amount());
                } else {
                    spendByCustomer.put(tx.customerId(), current.add(tx.amount()));
                }
            }
        }

        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(spendByCustomer.entrySet());
        entries.sort(new Comparator<Map.Entry<String, BigDecimal>>() {
            @Override
            public int compare(Map.Entry<String, BigDecimal> o1, Map.Entry<String, BigDecimal> o2) {
                return o2.getValue().compareTo(o1.getValue()); // Descending
            }
        });

        List<Map.Entry<String, BigDecimal>> result = new ArrayList<>();
        int count = Math.min(limit, entries.size());
        for (int i = 0; i < count; i++) {
            result.add(entries.get(i));
        }
        return result;
    }

    /**
     * Query 2: Monthly Total Spend across all successful transactions.
     * Collection choice:
     * - TreeMap<YearMonth, BigDecimal>: automatically keeps chronological order of months in O(log M).
     */
    @Override
    public Map<YearMonth, BigDecimal> getMonthlyTotals(List<Transaction> transactions) {
        Map<YearMonth, BigDecimal> monthlyTotals = new TreeMap<>();

        for (Transaction tx : transactions) {
            if ("SUCCESS".equalsIgnoreCase(tx.status())) {
                YearMonth month = tx.yearMonth();
                BigDecimal current = monthlyTotals.get(month);
                if (current == null) {
                    monthlyTotals.put(month, tx.amount());
                } else {
                    monthlyTotals.put(month, current.add(tx.amount()));
                }
            }
        }

        return monthlyTotals;
    }

    /**
     * Query 3: Duplicate Transactions.
     * Defined as multiple records having identical (customerId, timestamp, amount).
     * Collection choice:
     * - HashMap<String, List<Transaction>>: groups transactions sharing duplicate keys in O(1).
     * - ArrayList<Transaction>: collects duplicated instances.
     */
    @Override
    public List<Transaction> findDuplicates(List<Transaction> transactions) {
        Map<String, List<Transaction>> groupedByKey = new HashMap<>();

        for (Transaction tx : transactions) {
            String key = tx.duplicateKey();
            List<Transaction> bucket = groupedByKey.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                groupedByKey.put(key, bucket);
            }
            bucket.add(tx);
        }

        List<Transaction> duplicates = new ArrayList<>();
        for (List<Transaction> group : groupedByKey.values()) {
            if (group.size() > 1) {
                // If there are duplicates, we keep all records that are repeated beyond the first
                for (int i = 1; i < group.size(); i++) {
                    duplicates.add(group.get(i));
                }
            }
        }

        return duplicates;
    }

    /**
     * Query 4: Anomaly Detection.
     * Flags transactions whose amount exceeds threshold (e.g. >= 10,000.00).
     * Collection choice:
     * - ArrayList<Transaction>: simple dynamic list preserving encounter order with O(1) amortized appends.
     */
    @Override
    public List<Transaction> findAnomalies(List<Transaction> transactions, BigDecimal threshold) {
        List<Transaction> anomalies = new ArrayList<>();

        for (Transaction tx : transactions) {
            if (tx.amount().compareTo(threshold) >= 0) {
                anomalies.add(tx);
            }
        }

        return anomalies;
    }

    /**
     * Query 5: Spend Breakdown by Category.
     * Collection choice:
     * - HashMap<String, BigDecimal>: fast O(1) hash table for categorical accumulation.
     */
    @Override
    public Map<String, BigDecimal> getCategoryBreakdown(List<Transaction> transactions) {
        Map<String, BigDecimal> categoryTotals = new HashMap<>();

        for (Transaction tx : transactions) {
            if ("SUCCESS".equalsIgnoreCase(tx.status())) {
                String cat = tx.category();
                BigDecimal current = categoryTotals.get(cat);
                if (current == null) {
                    categoryTotals.put(cat, tx.amount());
                } else {
                    categoryTotals.put(cat, current.add(tx.amount()));
                }
            }
        }

        return categoryTotals;
    }

    /**
     * Query 6: Failure Rate by Merchant.
     * Computes the ratio of FAILED transactions per merchant.
     * Collection choice:
     * - HashMap<String, MerchantStats>: tracks total and failed counts in a single pass without redundant recalculations.
     */
    @Override
    public Map<String, Double> getFailureRateByMerchant(List<Transaction> transactions) {
        Map<String, int[]> counts = new HashMap<>(); // int[0] = failed, int[1] = total

        for (Transaction tx : transactions) {
            int[] stat = counts.get(tx.merchant());
            if (stat == null) {
                stat = new int[2];
                counts.put(tx.merchant(), stat);
            }
            stat[1]++; // increment total
            if ("FAILED".equalsIgnoreCase(tx.status())) {
                stat[0]++; // increment failed
            }
        }

        Map<String, Double> failureRates = new HashMap<>();
        for (Map.Entry<String, int[]> entry : counts.entrySet()) {
            int failed = entry.getValue()[0];
            int total = entry.getValue()[1];
            double rate = total == 0 ? 0.0 : (double) failed / total;
            failureRates.put(entry.getKey(), rate);
        }

        return failureRates;
    }
}
