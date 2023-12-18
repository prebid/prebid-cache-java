package org.prebid.cache.repository;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.redis.RedisRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ReactiveTestRedisRepositoryContext {
    @Bean
    @Primary
    public ReactiveRepository<PayloadWrapper, String> createRepository(RedisStringReactiveCommands<String, String> reactiveCommands) {
        return new RedisRepositoryImpl(reactiveCommands);
    }

    @Bean
    RedisClient client() {
        return RedisClient.create(RedisURI.Builder.redis("localhost", 6379).build());
    }

    @Bean
    StatefulRedisConnection<String, String> connection() {
        return client().connect();
    }

    @Bean
    RedisStringReactiveCommands<String, String> reactiveCommands() {
        return connection().reactive();
    }
}
