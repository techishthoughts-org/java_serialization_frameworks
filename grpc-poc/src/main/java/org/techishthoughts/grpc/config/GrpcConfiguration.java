package org.techishthoughts.grpc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.techishthoughts.payload.generator.PayloadGenerator;

/**
 * gRPC Application Configuration
 *
 * Provides beans and configuration for the gRPC serialization framework.
 */
@Configuration
public class GrpcConfiguration {

    /**
     * Provides PayloadGenerator bean for generating test data
     */
    @Bean
    public PayloadGenerator payloadGenerator() {
        return new PayloadGenerator();
    }
}
