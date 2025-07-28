package org.techishthoughts.jackson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.techishthoughts.jackson", "org.techishthoughts.payload"})
public class JacksonPocApplication {
    public static void main(String[] args) {
        SpringApplication.run(JacksonPocApplication.class, args);
    }
}
