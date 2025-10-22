package org.techishthoughts.fst.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

/**
 * FST Application Configuration
 *
 * Provides beans and configuration for the FST serialization framework.
 * UnifiedPayloadGenerator is now auto-configured via component scanning.
 */
@Configuration
public class FstConfiguration {

    /**
     * Provides PayloadGenerator bean for generating test data
     */
    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
