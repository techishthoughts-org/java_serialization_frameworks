package org.techishthoughts.sbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SBE Serialization Framework POC Application
 *
 * This application demonstrates SBE (Simple Binary Encoding) serialization capabilities
 * for ultra-low latency applications, particularly in financial trading systems.
 *
 * SBE is a binary format that:
 * - Provides ultra-low latency serialization
 * - Optimized for high-frequency trading
 * - Zero-copy access patterns
 * - Industry standard in financial markets
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.sbe", "org.techishthoughts.payload"})
public class SbePocApplication {

    public static void main(String[] args) {
        SpringApplication.run(SbePocApplication.class, args);
    }
}
