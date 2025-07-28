package org.techishthoughts.fst.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

@Configuration
public class FstConfiguration {

    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
