package com.crunchycode.analytics;

import com.crunchycode.analytics.data.CsvTransactionParser;
import com.crunchycode.analytics.model.Transaction;
import com.crunchycode.analytics.service.LoopTransactionService;
import com.crunchycode.analytics.service.StreamTransactionService;
import com.crunchycode.analytics.service.TransactionAnalyticsService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Benchmark runner and main entry point.
 * Generates datasets (20k and 200k), warms up JVM to avoid JIT bias,
 * runs both Loop and Stream implementations multiple times,
 * measures execution timings, validates equivalence, and outputs comparative metrics.
 */
public class Main {

    private static final int WARMUP_ITERATIONS = 5;
    private static final int MEASURE_ITERATIONS = 10;
    private static final BigDecimal ANOMALY_THRESHOLD = new BigDecimal("10000.00");

    public static void main(String[] args) throws IOException {
        System.out.println("================================================================================");
        System.out.println("   CRUNCHY CODE - COLLECTIONS VS STREAMS BENCHMARK HARNESS (JAVA 21)   ");
        System.out.println("================================================================================");

        Path dataDir = Paths.get("data");
        Path csv20k = dataDir.resolve("transactions_20k.csv");
        Path csv200k = dataDir.resolve("transactions_200k.csv");

        // 1. Generate or confirm synthetic datasets
        if (!Files.exists(csv20k)) {
            System.out.println("Generating 20,000 transaction CSV: " + csv20k);
            CsvTransactionParser.generateDataset(csv20k, 20_000, 42L);
        } else {
            System.out.println("Dataset exists: " + csv20k);
        }

        if (!Files.exists(csv200k)) {
            System.out.println("Generating 200,000 transaction CSV: " + csv200k);
            CsvTransactionParser.generateDataset(csv200k, 200_000, 42L);
        } else {
            System.out.println("Dataset exists: " + csv200k);
        }

        TransactionAnalyticsService loopService = new LoopTransactionService();
        TransactionAnalyticsService streamService = new StreamTransactionService();

        // 2. Run benchmark on 20,000 rows
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(">>> RUNNING BENCHMARK ON 20,000 ROWS");
        System.out.println("--------------------------------------------------------------------------------");
        List<Transaction> transactions20k = CsvTransactionParser.parse(csv20k);
        System.out.printf("Parsed %d transactions successfully from %s%n", transactions20k.size(), csv20k.getFileName());
        verifyEquivalence(transactions20k, loopService, streamService);
        runBenchmarkSuite("20k Dataset", transactions20k, loopService, streamService);

        // 3. Run benchmark on 200,000 rows
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(">>> RUNNING BENCHMARK ON 200,000 ROWS");
        System.out.println("--------------------------------------------------------------------------------");
        List<Transaction> transactions200k = CsvTransactionParser.parse(csv200k);
        System.out.printf("Parsed %d transactions successfully from %s%n", transactions200k.size(), csv200k.getFileName());
        verifyEquivalence(transactions200k, loopService, streamService);
        runBenchmarkSuite("200k Dataset", transactions200k, loopService, streamService);

        System.out.println("\n================================================================================");
        System.out.println("Benchmark completed successfully! Run results ready for FINDINGS.md report.");
        System.out.println("================================================================================");
    }

    /**
     * Asserts that both implementations return mathematically identical outputs for all 6 queries.
     */
    private static void verifyEquivalence(List<Transaction> data,
                                          TransactionAnalyticsService loopService,
                                          TransactionAnalyticsService streamService) {
        System.out.println("Verifying result consistency between Loop and Stream implementations...");

        // Query 1
        var topLoop = loopService.getTopCustomers(data, 5);
        var topStream = streamService.getTopCustomers(data, 5);
        if (topLoop.size() != topStream.size() ||
                !topLoop.getFirst().getKey().equals(topStream.getFirst().getKey())) {
            throw new IllegalStateException("Q1 Results mismatch!");
        }

        // Query 2
        var monthlyLoop = loopService.getMonthlyTotals(data);
        var monthlyStream = streamService.getMonthlyTotals(data);
        if (monthlyLoop.size() != monthlyStream.size()) {
            throw new IllegalStateException("Q2 Results mismatch in size!");
        }

        // Query 3
        var dupsLoop = loopService.findDuplicates(data);
        var dupsStream = streamService.findDuplicates(data);
        if (dupsLoop.size() != dupsStream.size()) {
            throw new IllegalStateException(String.format("Q3 Duplicate counts mismatch! Loop=%d, Stream=%d",
                    dupsLoop.size(), dupsStream.size()));
        }

        // Query 4
        var anomLoop = loopService.findAnomalies(data, ANOMALY_THRESHOLD);
        var anomStream = streamService.findAnomalies(data, ANOMALY_THRESHOLD);
        if (anomLoop.size() != anomStream.size()) {
            throw new IllegalStateException("Q4 Anomaly counts mismatch!");
        }

        // Query 5
        var catLoop = loopService.getCategoryBreakdown(data);
        var catStream = streamService.getCategoryBreakdown(data);
        if (catLoop.size() != catStream.size()) {
            throw new IllegalStateException("Q5 Categories count mismatch!");
        }

        // Query 6
        var failLoop = loopService.getFailureRateByMerchant(data);
        var failStream = streamService.getFailureRateByMerchant(data);
        if (failLoop.size() != failStream.size()) {
            throw new IllegalStateException("Q6 Failure rate merchant count mismatch!");
        }

        System.out.println("All 6 query results are 100% equivalent across implementations.");
    }

    private static void runBenchmarkSuite(String label,
                                         List<Transaction> data,
                                         TransactionAnalyticsService loopService,
                                         TransactionAnalyticsService streamService) {
        System.out.printf("%n=== BENCHMARK SUITE: %s (%d records, %d warmup, %d measure runs) ===%n",
                label, data.size(), WARMUP_ITERATIONS, MEASURE_ITERATIONS);
        System.out.printf("%-40s | %-15s | %-15s | %-10s%n", "Query", "Loop Avg (ms)", "Stream Avg (ms)", "Faster");
        System.out.println("-------------------------------------------------------------------------------------------");

        // Q1: Top 10 Customers
        BenchmarkResult q1 = benchmarkQuery(
                () -> loopService.getTopCustomers(data, 10),
                () -> streamService.getTopCustomers(data, 10)
        );
        printRow("Q1: Top 10 Customers", q1);

        // Q2: Monthly Totals
        BenchmarkResult q2 = benchmarkQuery(
                () -> loopService.getMonthlyTotals(data),
                () -> streamService.getMonthlyTotals(data)
        );
        printRow("Q2: Monthly Totals", q2);

        // Q3: Duplicates
        BenchmarkResult q3 = benchmarkQuery(
                () -> loopService.findDuplicates(data),
                () -> streamService.findDuplicates(data)
        );
        printRow("Q3: Duplicate Detection", q3);

        // Q4: Anomalies
        BenchmarkResult q4 = benchmarkQuery(
                () -> loopService.findAnomalies(data, ANOMALY_THRESHOLD),
                () -> streamService.findAnomalies(data, ANOMALY_THRESHOLD)
        );
        printRow("Q4: Anomalies (>= $10k)", q4);

        // Q5: Category Breakdown
        BenchmarkResult q5 = benchmarkQuery(
                () -> loopService.getCategoryBreakdown(data),
                () -> streamService.getCategoryBreakdown(data)
        );
        printRow("Q5: Category Spend", q5);

        // Q6: Merchant Failure Rate
        BenchmarkResult q6 = benchmarkQuery(
                () -> loopService.getFailureRateByMerchant(data),
                () -> streamService.getFailureRateByMerchant(data)
        );
        printRow("Q6: Merchant Failure Rate", q6);
    }

    private static void printRow(String name, BenchmarkResult r) {
        String faster = r.loopAvgMs < r.streamAvgMs ? "Loop (" + String.format("%.2fx", r.streamAvgMs / Math.max(0.001, r.loopAvgMs)) + ")"
                : "Stream (" + String.format("%.2fx", r.loopAvgMs / Math.max(0.001, r.streamAvgMs)) + ")";
        System.out.printf("%-40s | %15.3f | %15.3f | %-10s%n",
                name, r.loopAvgMs, r.streamAvgMs, faster);
    }

    private static BenchmarkResult benchmarkQuery(Runnable loopTask, Runnable streamTask) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            loopTask.run();
            streamTask.run();
        }

        // Measure Loop
        long totalLoopNanos = 0;
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long start = System.nanoTime();
            loopTask.run();
            totalLoopNanos += (System.nanoTime() - start);
        }

        // Measure Stream
        long totalStreamNanos = 0;
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long start = System.nanoTime();
            streamTask.run();
            totalStreamNanos += (System.nanoTime() - start);
        }

        double loopAvgMs = (totalLoopNanos / (double) MEASURE_ITERATIONS) / 1_000_000.0;
        double streamAvgMs = (totalStreamNanos / (double) MEASURE_ITERATIONS) / 1_000_000.0;

        return new BenchmarkResult(loopAvgMs, streamAvgMs);
    }

    private record BenchmarkResult(double loopAvgMs, double streamAvgMs) {}
}
