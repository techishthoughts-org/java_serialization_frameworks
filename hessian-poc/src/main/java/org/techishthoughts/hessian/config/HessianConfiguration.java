package org.techishthoughts.hessian.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

/**
 * Hessian Application Configuration
 *
 * Provides beans and configuration for the Hessian serialization framework.
 */
@Configuration
public class HessianConfiguration {

    /**
     * Provides PayloadGenerator bean for generating test data
     */
    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
