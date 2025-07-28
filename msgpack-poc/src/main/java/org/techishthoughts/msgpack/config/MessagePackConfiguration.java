package org.techishthoughts.msgpack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

/**
 * MessagePack Application Configuration
 *
 * Provides beans and configuration for the MessagePack serialization framework.
 */
@Configuration
public class MessagePackConfiguration {

    /**
     * Provides PayloadGenerator bean for generating test data
     */
    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
