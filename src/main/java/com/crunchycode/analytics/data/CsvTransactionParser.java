package com.crunchycode.analytics.data;

import com.crunchycode.analytics.model.Transaction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility for parsing CSV transaction files and generating realistic synthetic datasets
 * with natural patterns: top customer concentrations, seasonal months, duplicates, and anomalies.
 */
public class CsvTransactionParser {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Parses a CSV file into a list of Transaction records.
     * Handles standard header skip and field validation.
     */
    public static List<Transaction> parse(Path csvPath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // Header
            if (line == null) {
                return transactions;
            }

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 7) {
                    continue; // Skip malformed rows
                }

                try {
                    String txId = parts[0].trim();
                    String customerId = parts[1].trim();
                    LocalDateTime timestamp = LocalDateTime.parse(parts[2].trim(), DATE_TIME_FORMATTER);
                    BigDecimal amount = new BigDecimal(parts[3].trim());
                    String category = parts[4].trim();
                    String status = parts[5].trim();
                    String merchant = parts[6].trim();

                    transactions.add(new Transaction(txId, customerId, timestamp, amount, category, status, merchant));
                } catch (Exception e) {
                    // Log or handle individual parsing error without crashing the full batch
                    System.err.println("Warning: Skipping malformed row at line " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        return transactions;
    }

    /**
     * Generates a realistic CSV file with a given count of transactions.
     * Predictable seed ensures reproducibility across benchmarking runs.
     */
    public static void generateDataset(Path outputPath, int count, long seed) throws IOException {
        Files.createDirectories(outputPath.getParent());
        Random random = new Random(seed);

        String[] categories = {"GROCERIES", "ELECTRONICS", "TRAVEL", "ENTERTAINMENT", "HEALTHCARE", "UTILITIES", "DINING"};
        String[] statuses = {"SUCCESS", "SUCCESS", "SUCCESS", "SUCCESS", "PENDING", "FAILED"};
        String[] merchants = {"Amazon", "Apple", "Walmart", "Netflix", "Uber", "Target", "Starbucks", "Shell", "Costco", "Delta"};

        // 1,000 unique customers with a Pareto distribution (top 50 make lots of transactions)
        int customerCount = Math.max(100, count / 100);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write("transactionId,customerId,timestamp,amount,category,status,merchant");
            writer.newLine();

            LocalDateTime baseTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

            // Buffer to hold generated rows so we can inject realistic duplicates
            List<String> recentRows = new ArrayList<>(500);

            for (int i = 1; i <= count; i++) {
                // Determine duplicate injection (about 0.5% duplicates)
                if (i > 100 && !recentRows.isEmpty() && random.nextDouble() < 0.005) {
                    String dupTemplate = recentRows.get(random.nextInt(recentRows.size()));
                    String[] parts = dupTemplate.split(",");
                    // Give new unique transactionId, but same customerId, timestamp, amount
                    String dupRow = String.format("TX-%08d,%s,%s,%s,%s,%s,%s",
                            i, parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
                    writer.write(dupRow);
                    writer.newLine();
                    continue;
                }

                // Skew customer distribution: 20% of customers create 60% of volume
                int custIdNum;
                if (random.nextDouble() < 0.60) {
                    custIdNum = 1 + random.nextInt(Math.max(1, customerCount / 10)); // heavy buyers
                } else {
                    custIdNum = 1 + random.nextInt(customerCount);
                }
                String custId = String.format("CUST-%05d", custIdNum);

                // Timestamp spread over 12 months of 2025
                int dayOffset = random.nextInt(365);
                int secondOffset = random.nextInt(86400);
                LocalDateTime txTime = baseTime.plusDays(dayOffset).plusSeconds(secondOffset);

                // Amount calculation: typical retail $5 - $350, with rare extreme outliers / anomalies ($10,000+)
                BigDecimal amount;
                if (random.nextDouble() < 0.0008) {
                    // Outlier / Anomaly (> $10,000)
                    amount = BigDecimal.valueOf(10000.0 + random.nextDouble() * 40000.0).setScale(2, RoundingMode.HALF_UP);
                } else {
                    // Regular retail purchase
                    amount = BigDecimal.valueOf(2.50 + Math.pow(random.nextDouble(), 2) * 500.0).setScale(2, RoundingMode.HALF_UP);
                }

                String category = categories[random.nextInt(categories.length)];
                String status = statuses[random.nextInt(statuses.length)];
                String merchant = merchants[random.nextInt(merchants.length)];

                String row = String.format("TX-%08d,%s,%s,%s,%s,%s,%s",
                        i, custId, txTime.format(DATE_TIME_FORMATTER), amount.toPlainString(), category, status, merchant);

                writer.write(row);
                writer.newLine();

                if (recentRows.size() < 400) {
                    recentRows.add(row);
                } else {
                    recentRows.set(random.nextInt(recentRows.size()), row);
                }
            }
        }
    }
}
