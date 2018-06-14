package org.prebid.cache.repository;

import org.prebid.cache.repository.redis.RedisPropertyConfiguration;
import org.prebid.cache.repository.redis.RedisRepositoryImpl;
import org.prebid.cache.repository.redis.RedisSentinelPropertyConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ReactiveTestRedisRepositoryContext {
    @Bean
    @Primary
    public ReactiveRepository createRepository() {
        return new RedisRepositoryImpl(new RedisPropertyConfiguration(), new RedisSentinelPropertyConfiguration());
    }
}
