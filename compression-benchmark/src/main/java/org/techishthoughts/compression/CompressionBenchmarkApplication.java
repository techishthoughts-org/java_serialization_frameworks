package org.techishthoughts.compression;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.compression", "org.techishthoughts.payload"})
public class CompressionBenchmarkApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompressionBenchmarkApplication.class, args);
    }
}
