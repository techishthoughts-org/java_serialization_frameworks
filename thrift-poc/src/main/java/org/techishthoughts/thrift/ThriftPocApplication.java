package org.techishthoughts.thrift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Apache Thrift POC Application
 *
 * 2025 UPDATE: Apache Thrift provides cross-language RPC with
 * high-performance serialization and battle-tested reliability.
 */
@SpringBootApplication
public class ThriftPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThriftPocApplication.class, args);
    }
}
