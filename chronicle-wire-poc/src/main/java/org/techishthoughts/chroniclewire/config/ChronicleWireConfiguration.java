package org.techishthoughts.chroniclewire.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

@Configuration
public class ChronicleWireConfiguration {

    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
