package org.techishthoughts.cbor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CBOR Serialization Framework POC Application
 *
 * This application demonstrates CBOR (Concise Binary Object Representation)
 * serialization capabilities for IoT and constrained environments.
 *
 * CBOR is an IETF standard binary format that is:
 * - Compact and efficient
 * - Self-describing
 * - Language-agnostic
 * - Perfect for IoT devices and constrained environments
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.cbor", "org.techishthoughts.payload"})
public class CborPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(CborPocApplication.class, args);
    }
}
