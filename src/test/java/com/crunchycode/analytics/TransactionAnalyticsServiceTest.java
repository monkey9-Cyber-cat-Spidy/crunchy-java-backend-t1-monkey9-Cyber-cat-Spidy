package com.crunchycode.analytics;

import com.crunchycode.analytics.model.Transaction;
import com.crunchycode.analytics.service.LoopTransactionService;
import com.crunchycode.analytics.service.StreamTransactionService;
import com.crunchycode.analytics.service.TransactionAnalyticsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionAnalyticsServiceTest {

    private static List<Transaction> sampleData;
    private final TransactionAnalyticsService loopService = new LoopTransactionService();
    private final TransactionAnalyticsService streamService = new StreamTransactionService();

    @BeforeAll
    public static void setup() {
        sampleData = new ArrayList<>();
        LocalDateTime dt1 = LocalDateTime.of(2025, 1, 15, 10, 0, 0);
        LocalDateTime dt2 = LocalDateTime.of(2025, 2, 20, 14, 30, 0);
        LocalDateTime dt3 = LocalDateTime.of(2025, 3, 5, 9, 15, 0);

        sampleData.add(new Transaction("TX-1", "C-01", dt1, new BigDecimal("100.00"), "GROCERIES", "SUCCESS", "Walmart"));
        sampleData.add(new Transaction("TX-2", "C-01", dt2, new BigDecimal("250.00"), "ELECTRONICS", "SUCCESS", "Apple"));
        // Duplicate of TX-1 (same customer, timestamp, amount)
        sampleData.add(new Transaction("TX-3", "C-01", dt1, new BigDecimal("100.00"), "GROCERIES", "SUCCESS", "Walmart"));
        // Failed transaction
        sampleData.add(new Transaction("TX-4", "C-02", dt2, new BigDecimal("500.00"), "TRAVEL", "FAILED", "Delta"));
        // Large anomaly
        sampleData.add(new Transaction("TX-5", "C-02", dt3, new BigDecimal("15000.00"), "HEALTHCARE", "SUCCESS", "Hospital"));
        sampleData.add(new Transaction("TX-6", "C-03", dt3, new BigDecimal("50.00"), "DINING", "SUCCESS", "Starbucks"));
    }

    @Test
    public void testTopCustomersEquivalence() {
        var loop = loopService.getTopCustomers(sampleData, 3);
        var stream = streamService.getTopCustomers(sampleData, 3);

        assertEquals(loop.size(), stream.size());
        for (int i = 0; i < loop.size(); i++) {
            assertEquals(loop.get(i).getKey(), stream.get(i).getKey());
            assertEquals(0, loop.get(i).getValue().compareTo(stream.get(i).getValue()));
        }
    }

    @Test
    public void testMonthlyTotalsEquivalence() {
        var loop = loopService.getMonthlyTotals(sampleData);
        var stream = streamService.getMonthlyTotals(sampleData);

        assertEquals(loop.keySet(), stream.keySet());
        for (YearMonth ym : loop.keySet()) {
            assertEquals(0, loop.get(ym).compareTo(stream.get(ym)));
        }
    }

    @Test
    public void testDuplicatesEquivalence() {
        var loop = loopService.findDuplicates(sampleData);
        var stream = streamService.findDuplicates(sampleData);

        assertEquals(1, loop.size());
        assertEquals(1, stream.size());
        assertEquals(loop.get(0).transactionId(), stream.get(0).transactionId());
    }

    @Test
    public void testAnomaliesEquivalence() {
        BigDecimal threshold = new BigDecimal("10000.00");
        var loop = loopService.findAnomalies(sampleData, threshold);
        var stream = streamService.findAnomalies(sampleData, threshold);

        assertEquals(1, loop.size());
        assertEquals(1, stream.size());
        assertEquals("TX-5", loop.get(0).transactionId());
        assertEquals("TX-5", stream.get(0).transactionId());
    }

    @Test
    public void testCategoryBreakdownEquivalence() {
        var loop = loopService.getCategoryBreakdown(sampleData);
        var stream = streamService.getCategoryBreakdown(sampleData);

        assertEquals(loop.keySet(), stream.keySet());
        for (String cat : loop.keySet()) {
            assertEquals(0, loop.get(cat).compareTo(stream.get(cat)));
        }
    }

    @Test
    public void testFailureRateByMerchantEquivalence() {
        var loop = loopService.getFailureRateByMerchant(sampleData);
        var stream = streamService.getFailureRateByMerchant(sampleData);

        assertEquals(loop.keySet(), stream.keySet());
        for (String merchant : loop.keySet()) {
            assertEquals(loop.get(merchant), stream.get(merchant), 0.0001);
        }
    }
}
