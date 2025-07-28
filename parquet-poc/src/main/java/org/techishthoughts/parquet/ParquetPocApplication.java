package org.techishthoughts.parquet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Apache Parquet Serialization Framework POC Application
 *
 * This application demonstrates Apache Parquet serialization capabilities
 * for data warehousing and analytics workloads.
 *
 * Apache Parquet is a columnar storage format that:
 * - Provides excellent compression ratios
 * - Optimized for analytical queries
 * - Self-describing format
 * - Industry standard for data lakes
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.parquet", "org.techishthoughts.payload"})
public class ParquetPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParquetPocApplication.class, args);
    }
}
