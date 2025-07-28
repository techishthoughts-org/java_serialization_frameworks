package org.techishthoughts.capnproto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

/**
 * Cap'n Proto Application Configuration
 *
 * Provides beans and configuration for the Cap'n Proto serialization framework.
 */
@Configuration
public class CapnProtoConfiguration {

    /**
     * Provides PayloadGenerator bean for generating test data
     */
    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
