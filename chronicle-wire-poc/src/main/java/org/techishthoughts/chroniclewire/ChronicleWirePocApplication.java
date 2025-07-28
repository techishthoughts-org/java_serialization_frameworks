package org.techishthoughts.chroniclewire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chronicle Wire POC Application (2025)
 *
 * Ultra-low latency serialization framework designed for:
 * - High-frequency trading applications
 * - Real-time streaming systems
 * - Zero-garbage-collection serialization
 * - Sub-microsecond performance
 * - Memory-mapped file persistence
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.chroniclewire", "org.techishthoughts.payload"})
public class ChronicleWirePocApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronicleWirePocApplication.class, args);
    }
}
