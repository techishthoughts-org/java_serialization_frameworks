package org.techishthoughts.messagepack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Enhanced MessagePack POC Application (2025)
 *
 * Enhanced MessagePack implementation with:
 * - Jackson integration for better object mapping
 * - Multiple compression algorithms (LZ4, Snappy, ZSTD)
 * - Streaming support for large datasets
 * - Schema evolution capabilities
 * - Zero-copy optimizations
 * - Custom serializers for Java 8+ time classes
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.messagepack", "org.techishthoughts.payload"})
public class MessagePackEnhancedPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagePackEnhancedPocApplication.class, args);
    }
}
