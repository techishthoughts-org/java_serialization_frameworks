package org.techishthoughts.grpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * gRPC POC Application
 *
 * Spring Boot application for gRPC serialization benchmarking.
 * Provides REST endpoints for testing gRPC performance against other frameworks.
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.grpc", "org.techishthoughts.payload"})
@ComponentScan(basePackages = {"org.techishthoughts.grpc", "org.techishthoughts.payload"})
public class GrpcPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcPocApplication.class, args);
    }
}
