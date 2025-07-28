package org.techishthoughts.arrow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Apache Arrow Serialization Framework POC Application
 *
 * This application demonstrates Apache Arrow serialization capabilities
 * for big data and analytics workloads.
 *
 * Apache Arrow is a columnar in-memory data format that:
 * - Enables efficient data sharing across systems
 * - Optimized for analytical workloads
 * - Language-agnostic and platform-independent
 * - Industry standard for big data processing
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.arrow", "org.techishthoughts.payload"})
public class ArrowPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArrowPocApplication.class, args);
    }
}
