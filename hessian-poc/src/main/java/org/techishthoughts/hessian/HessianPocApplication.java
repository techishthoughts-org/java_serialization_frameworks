package org.techishthoughts.hessian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hessian POC Application
 *
 * 2025 UPDATE: Hessian provides binary web service protocol with
 * high-performance serialization and wide language support.
 */
@SpringBootApplication
public class HessianPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(HessianPocApplication.class, args);
    }
}
