package org.techishthoughts.protobuf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.techishthoughts.protobuf", "org.techishthoughts.payload"})
public class ProtobufPocApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProtobufPocApplication.class, args);
    }
}
