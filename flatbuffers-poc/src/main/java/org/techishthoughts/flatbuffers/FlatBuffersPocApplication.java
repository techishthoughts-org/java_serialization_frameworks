package org.techishthoughts.flatbuffers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * FlatBuffers POC Application
 *
 * Spring Boot application for FlatBuffers serialization benchmarking.
 * Provides REST endpoints for testing FlatBuffers performance against other frameworks.
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.flatbuffers", "org.techishthoughts.payload"})
@ComponentScan(basePackages = {"org.techishthoughts.flatbuffers", "org.techishthoughts.payload"})
public class FlatBuffersPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlatBuffersPocApplication.class, args);
    }
}
