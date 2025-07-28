package org.techishthoughts.fst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Enhanced FST POC Application (2025)
 *
 * Enhanced FST (Fast Serialization) implementation with:
 * - GraalVM native image support for ultra-fast startup
 * - Off-heap storage with Chronicle Map integration
 * - Advanced compression algorithms
 * - Streaming serialization for large datasets
 * - Zero-copy optimizations where possible
 * - Enhanced Java serialization alternative
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.fst", "org.techishthoughts.payload"})
public class FstEnhancedPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(FstEnhancedPocApplication.class, args);
    }
}
