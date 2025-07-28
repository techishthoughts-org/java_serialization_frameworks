package org.techishthoughts.fory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.techishthoughts.fory", "org.techishthoughts.payload"})
public class ForyPocApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForyPocApplication.class, args);
    }
}
