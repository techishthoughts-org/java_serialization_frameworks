package org.techishthoughts.capnproto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Cap'n Proto POC Application
 *
 * 2025 UPDATE: Cap'n Proto provides zero-copy serialization with
 * exceptional performance and schema evolution capabilities.
 */
@SpringBootApplication(scanBasePackages = {"org.techishthoughts.capnproto", "org.techishthoughts.payload"})
public class CapnProtoPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(CapnProtoPocApplication.class, args);
    }
}
