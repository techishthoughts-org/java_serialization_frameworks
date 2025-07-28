package org.techishthoughts.kryo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.techishthoughts.kryo", "org.techishthoughts.payload"})
public class KryoPocApplication {
    public static void main(String[] args) {
        SpringApplication.run(KryoPocApplication.class, args);
    }
}
