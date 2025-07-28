package org.techishthoughts.bson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BSON Serialization Framework POC Application
 *
 * This application demonstrates BSON (Binary JSON) serialization capabilities
 * for MongoDB ecosystem and document databases.
 *
 * BSON is MongoDB's binary format that:
 * - Extends JSON with additional data types
 * - Provides efficient binary encoding
 * - Supports rich document structures
 * - Optimized for MongoDB operations
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.bson", "org.techishthoughts.payload"})
public class BsonPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(BsonPocApplication.class, args);
    }
}
