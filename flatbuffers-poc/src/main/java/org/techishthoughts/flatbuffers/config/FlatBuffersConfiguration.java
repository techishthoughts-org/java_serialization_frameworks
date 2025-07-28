package org.techishthoughts.flatbuffers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

/**
 * FlatBuffers Application Configuration
 *
 * Provides beans and configuration for the FlatBuffers serialization framework.
 */
@Configuration
public class FlatBuffersConfiguration {

    /**
     * Provides PayloadGenerator bean for generating test data
     */
    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
