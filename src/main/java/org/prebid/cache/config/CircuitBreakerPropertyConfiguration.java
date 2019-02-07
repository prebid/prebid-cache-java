package org.prebid.cache.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakerHealthIndicator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "circuitbreaker")
public class CircuitBreakerPropertyConfiguration {

    private static final String CIRCUIT_BREAKER_NAME = "prebid-cache-circuit-breaker";

    private int failureRateThreshold;
    private long openStateDuration;
    private int closedStateCallsNumber;
    private int halfOpenStateCallsNumber;

    @Bean
    CircuitBreakerConfig config() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofMillis(getOpenStateDuration()))
                .ringBufferSizeInHalfOpenState(getHalfOpenStateCallsNumber())
                .ringBufferSizeInClosedState(getClosedStateCallsNumber())
                .build();
    }

    @Bean
    CircuitBreakerRegistry registry() {
        return CircuitBreakerRegistry.of(config());
    }

    @Bean
    CircuitBreaker circuitBreaker() {
        return registry().circuitBreaker(CIRCUIT_BREAKER_NAME, config());
    }

    @Bean
    CircuitBreakerHealthIndicator healthIndicator() {
        return new CircuitBreakerHealthIndicator(circuitBreaker());
    }
}
