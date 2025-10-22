package org.techishthoughts.fst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FST (Fast-Serialization) POC Application
 *
 * Tech.ish Thoughts Organization
 * https://github.com/techishthoughts-org/compression-java-strategy
 *
 * This application demonstrates FST high-performance Java serialization
 * for in-memory databases and caching in 2025.
 *
 * Features:
 * - Extremely fast Java serialization (3-5x faster than Java serialization)
 * - Memory-efficient operations
 * - Optimized for in-memory databases
 * - Caching-friendly serialization
 * - Integration with modern compression algorithms
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.fst", "org.techishthoughts.payload"})
public class FstPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(FstPocApplication.class, args);
    }
}
