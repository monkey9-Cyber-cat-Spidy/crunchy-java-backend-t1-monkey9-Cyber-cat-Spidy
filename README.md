# Crunchy Code Internship — Java Backend Task 1

Candidate handle: `monkey9-Cyber-cat-Spidy`

## Overview
This repository contains the complete implementation and empirical benchmark comparing the **Java Collections Framework (imperative loops)** against the **Streams API (declarative pipelines)** across a dataset of **20,000** and **200,000** transaction records.

## Project Structure
```text
crunchy-java-backend-t1-monkey9-Cyber-cat-Spidy/
├── pom.xml
├── FINDINGS.md                           # Complete timings, answers & analysis
├── README.md                             # Setup & run instructions
├── data/
│   ├── transactions_20k.csv              # Generated 20k rows dataset
│   └── transactions_200k.csv             # Generated 200k rows dataset
└── src/
    ├── main/java/com/crunchycode/analytics/
    │   ├── Main.java                     # Benchmark execution harness
    │   ├── model/
    │   │   └── Transaction.java          # Domain record
    │   ├── service/
    │   │   ├── TransactionAnalyticsService.java  # Analytical contract
    │   │   ├── LoopTransactionService.java       # Imperative loop implementation
    │   │   └── StreamTransactionService.java     # Declarative Stream implementation
    │   └── data/
    │       └── CsvTransactionParser.java         # CSV parser and dataset generator
    └── test/java/com/crunchycode/analytics/
        └── TransactionAnalyticsServiceTest.java  # 100% equivalence assertions
```

## Prerequisites
- Java 21 (LTS)
- Apache Maven 3.9+

## Running the Project

### 1. Verify Maven & Java 21
```powershell
mvn -v
```

### 2. Run Tests
```powershell
mvn test
```

### 3. Run Benchmark Suite (20k & 200k)
```powershell
mvn package
java -cp target/crunchy-java-backend-t1-monkey9-Cyber-cat-Spidy-1.0.0.jar com.crunchycode.analytics.Main
```

Detailed findings, collection choices, and speedup breakdowns are recorded in [FINDINGS.md](FINDINGS.md).
