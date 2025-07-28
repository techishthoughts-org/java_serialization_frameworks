package org.techishthoughts.thrift.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

/**
 * Thrift Application Configuration
 *
 * Provides beans and configuration for the Thrift serialization framework.
 */
@Configuration
public class ThriftConfiguration {

    /**
     * Provides PayloadGenerator bean for generating test data
     */
    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
