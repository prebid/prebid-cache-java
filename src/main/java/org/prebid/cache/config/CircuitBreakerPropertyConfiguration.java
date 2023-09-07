package org.prebid.cache.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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

    private static final String WEB_CLIENT_CIRCUIT_BREAKER_NAME = "prebid-cache-web-client-circuit-breaker";
    private static final String REPOSITORY_CIRCUIT_BREAKER_NAME = "prebid-cache-repository-circuit-breaker";

    private int failureRateThreshold;
    private long openStateDuration;
    private int closedStateCallsNumber;
    private int halfOpenStateCallsNumber;

    @Bean
    CircuitBreakerConfig config() {
        int slidingWindowSize = getClosedStateCallsNumber();
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofMillis(getOpenStateDuration()))
                .permittedNumberOfCallsInHalfOpenState(getHalfOpenStateCallsNumber())
                .slidingWindow(slidingWindowSize, slidingWindowSize, CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();
    }

    @Bean
    CircuitBreakerRegistry registry() {
        return CircuitBreakerRegistry.of(config());
    }

    @Bean
    CircuitBreaker webClientCircuitBreaker() {
        return registry().circuitBreaker(WEB_CLIENT_CIRCUIT_BREAKER_NAME, config());
    }

    @Bean
    CircuitBreaker repositoryCircuitBreaker() {
        return registry().circuitBreaker(REPOSITORY_CIRCUIT_BREAKER_NAME, config());
    }
}
