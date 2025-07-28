package org.techishthoughts.msgpack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MessagePack POC Application
 *
 * 2025 UPDATE: MessagePack provides fast binary serialization with
 * excellent performance and cross-language compatibility.
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.msgpack", "org.techishthoughts.payload"})
public class MessagePackPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagePackPocApplication.class, args);
    }
}
