package org.prebid.cache.config;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.policy.Policy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.CircuitBreakerSecuredReactiveRepository;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.repository.TimeOutCapableReactiveRepository;
import org.prebid.cache.repository.aerospike.AerospikePropertyConfiguration;
import org.prebid.cache.repository.aerospike.AerospikeRepositoryImpl;
import org.prebid.cache.repository.redis.RedisRepositoryImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class RepositoryConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.redis", name = {"timeout"})
    ReactiveRepository<PayloadWrapper, String> redisRepository(
            RedisStringReactiveCommands<String, String> redisReactiveCommands) {

        return new RedisRepositoryImpl(redisReactiveCommands);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.aerospike", name = {"host"})
    ReactiveRepository<PayloadWrapper, String> aerospikeRepository(AerospikePropertyConfiguration configuration,
                                                                   AerospikeClient client,
                                                                   EventLoops eventLoops,
                                                                   Policy policy) {

        return new AerospikeRepositoryImpl(configuration, client, eventLoops, policy);
    }

    @Bean
    @Primary
    ReactiveRepository<PayloadWrapper, String> circuitBreakerSecuredRepository(
            ReactiveRepository<PayloadWrapper, String> repository,
            CircuitBreaker repositoryCircuitBreaker,
            CacheConfig config) {

        final var timeoutDecorator = new TimeOutCapableReactiveRepository<>(
                repository, Duration.ofMillis(config.getTimeoutMs()));

        return new CircuitBreakerSecuredReactiveRepository<>(timeoutDecorator, repositoryCircuitBreaker);
    }
}
