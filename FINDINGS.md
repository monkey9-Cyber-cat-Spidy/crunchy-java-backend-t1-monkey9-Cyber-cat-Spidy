# Crunchy Code Internship — Collections Framework vs Streams API

**Candidate Handle**: `monkey9-Cyber-cat-Spidy`  
**Project**: `crunchy-java-backend-t1-monkey9-Cyber-cat-Spidy`  
**Runtime**: Java 21 LTS (`Oracle JDK 21.0.9`), Apache Maven 3.9.12  
**Dataset Sizes Tested**: 20,000 rows (~1.5 MB) and 200,000 rows (~15.1 MB)

---

## 1. Environment & Setup Evidence (`mvn -v`)

```text
Apache Maven 3.9.12 (848fbb4bf2d427b72bdb2471c22fced7ebd9a7a1)
Maven home: C:\Program Files\apache-maven-3.9.12
Java version: 21.0.9, vendor: Oracle Corporation, runtime: C:\Program Files\Java\jdk-21
Default locale: en_IN, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
```

---

## 2. The Six Analytical Queries & Collection Choices

Each query was implemented twice: once using imperative loops with direct collection manipulation, and once using the declarative Java 21 Streams API and Collectors.

| Query | Analytical Objective | Chosen Collection | One-Line Justification |
| :--- | :--- | :--- | :--- |
| **Q1** | **Top N Customers** (by spend) | `HashMap<String, BigDecimal>` + `ArrayList<Map.Entry>` | `HashMap` gives $O(1)$ amortized lookups to accumulate spend, while `ArrayList` allows fast in-place comparison sorting. |
| **Q2** | **Monthly Totals** (chronological) | `TreeMap<YearMonth, BigDecimal>` | `TreeMap` keeps aggregated months naturally sorted in chronological order via red-black tree ($O(\log M)$ insertion). |
| **Q3** | **Duplicate Detection** | `HashMap<String, List<Transaction>>` | Fast $O(1)$ hashing buckets records sharing identical `(customerId, timestamp, amount)` collision keys. |
| **Q4** | **Anomaly Detection** (spend $\ge \$10\text{k}$) | `ArrayList<Transaction>` | Lightweight contiguous array preserving encounter order with minimal overhead for filtered elements. |
| **Q5** | **Category Spend Breakdown** | `HashMap<String, BigDecimal>` | Low-cardinality discrete category groupings benefit from instantaneous $O(1)$ key lookups with no sort overhead. |
| **Q6** | **Merchant Failure Rate** | `HashMap<String, int[]>` (counts) $\to$ `HashMap<String, Double>` | A single hash map holds fixed-size primitive count arrays (`[failed, total]`) avoid secondary map passes or boxing. |

---

## 3. Benchmark Timings: Loop vs Stream

Benchmarks executed with 5 JVM warm-up iterations followed by 10 measured iterations under identical memory and CPU conditions.

### Results on 20,000 Rows

| Query | Loop Avg (ms) | Stream Avg (ms) | Faster Approach | Speedup |
| :--- | :---: | :---: | :---: | :---: |
| **Q1: Top 10 Customers** | 2.335 ms | 2.267 ms | **Stream** | 1.03x |
| **Q2: Monthly Totals** | 0.792 ms | 1.659 ms | **Loop** | 2.09x |
| **Q3: Duplicate Detection** | 7.019 ms | 5.455 ms | **Stream** | 1.29x |
| **Q4: Anomalies ($\ge \$10\text{k}$)** | 0.196 ms | 0.134 ms | **Stream** | 1.46x |
| **Q5: Category Spend** | 0.796 ms | 0.479 ms | **Stream** | 1.66x |
| **Q6: Merchant Failure Rate** | 0.613 ms | 0.996 ms | **Loop** | 1.62x |

### Results on 200,000 Rows

| Query | Loop Avg (ms) | Stream Avg (ms) | Faster Approach | Speedup |
| :--- | :---: | :---: | :---: | :---: |
| **Q1: Top 10 Customers** | 19.613 ms | 23.607 ms | **Loop** | 1.20x |
| **Q2: Monthly Totals** | 24.407 ms | 27.538 ms | **Loop** | 1.13x |
| **Q3: Duplicate Detection** | 122.837 ms | 142.516 ms | **Loop** | 1.16x |
| **Q4: Anomalies ($\ge \$10\text{k}$)** | 5.577 ms | 8.570 ms | **Loop** | 1.54x |
| **Q5: Category Spend** | 23.462 ms | 34.663 ms | **Loop** | 1.48x |
| **Q6: Merchant Failure Rate** | 13.499 ms | 26.492 ms | **Loop** | 1.96x |

---

## 4. Performance & Readability Analysis: Which was faster? Which was clearer?

### 1. Speed Comparison: The Scale Shift
- **At 20k rows**: The Stream API was slightly faster or comparable on several queries (Q1, Q3, Q4, Q5). At smaller volumes, the JVM JIT compiler can aggressively inline stream stages, and memory pressure is low.
- **At 200k rows**: **Loops were faster across all 6 queries** (between 1.13x and 1.96x faster).
- **Why did Loops win at 200k rows?**
  1. **Object Allocation & Boxing Overhead**: Streams pipeline elements through `Spliterator`, intermediate `Stream` stages, and functional interfaces (`Predicate`, `Function`, `Collector`).
  2. **Collector Boilerplate**: `Collectors.reducing` with `BigDecimal::add` repeatedly boxes and wraps accumulators, whereas the loop version directly manipulates map values via straightforward `map.put(key, current.add(amount))`.
  3. **Multi-pass Collector Costs (Q6)**: In the Stream implementation of Merchant Failure Rate, grouping elements into temporary lists before computing counts creates substantial GC churn ($26.49\text{ ms}$ vs $13.50\text{ ms}$). The loop version maintains a simple `int[2]` state array in a single sequential pass.

---

### 2. Readability Comparison: Where Streams Won vs Where They Cost Readability

#### Where Streams WON in Readability
- **Query 4 (Filtering Anomalies)**:
  ```java
  // Stream version: Concise, self-explanatory intent
  return transactions.stream()
          .filter(tx -> tx.amount().compareTo(threshold) >= 0)
          .toList();
  ```
  The Stream pipeline is superior here because it expresses *what* is being extracted rather than *how* the accumulator array is being populated.

- **Query 2 (Monthly Totals with Ordered Map)**:
  Using `Collectors.groupingBy(Transaction::yearMonth, TreeMap::new, ...)` cleanly states grouping and sorting target in one declaration.

---

#### Where Streams COST Readability & Maintainability
- **Query 6 (Merchant Failure Rate)**:
  - *Stream Version*: Requires nested collectors (`groupingBy` composed with `collectingAndThen` or downstream list collectors), making the type signature and lambda nesting dense and difficult for team members to digest at a glance:
    ```java
    transactions.stream()
        .collect(Collectors.groupingBy(
            Transaction::merchant,
            Collectors.collectingAndThen(
                Collectors.toList(),
                list -> (double) list.stream().filter(tx -> "FAILED".equalsIgnoreCase(tx.status())).count() / list.size()
            )
        ));
    ```
  - *Loop Version*: Clear, humanized step-by-step logic:
    ```java
    for (Transaction tx : transactions) {
        int[] stat = counts.computeIfAbsent(tx.merchant(), k -> new int[2]);
        stat[1]++;
        if ("FAILED".equalsIgnoreCase(tx.status())) stat[0]++;
    }
    ```
    The loop is easier to debug with a breakpoint, uses less memory, and is immediately understandable by any Java engineer without deciphering complex generic collector chains.

- **Query 3 (Duplicate Extraction)**:
  Flattener pipelines (`.values().stream().filter(...).flatMap(...skip(1))`) require deep mental modeling compared to a straightforward group-and-inspect loop.

---

## 5. Summary Conclusion

1. **Streams API** provides unmatched declarative elegance for linear filters, simple transformations, and basic groupings.
2. **Loops + Direct Collections** provide superior throughput, zero pipeline overhead, predictable memory allocation, and cleaner readability whenever multiple aggregates or complex downstream evaluations (like ratios and duplicates) are needed.
3. **Measure before optimizing**: As evidenced by the 20k vs 200k benchmark shifts, micro-benchmarks at small scales can be deceptive. Real-world volume exposes GC pressure and abstraction costs.
