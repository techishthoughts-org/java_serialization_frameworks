package org.techishthoughts.avro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.techishthoughts.avro", "org.techishthoughts.payload"})
public class AvroPocApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvroPocApplication.class, args);
    }
}
