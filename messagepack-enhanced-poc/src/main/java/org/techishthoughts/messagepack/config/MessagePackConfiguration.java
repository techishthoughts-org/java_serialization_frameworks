package org.techishthoughts.messagepack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

@Configuration
public class MessagePackConfiguration {

    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
