package org.prebid.cache.repository.redis.module.storage;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.repository.redis.RedisConfigurationProperties;
import org.prebid.cache.repository.redis.RedisRepositoryImpl;
import org.prebid.cache.repository.redis.RedisUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class ModuleCompositeRepositoryConfiguration {

    @Bean
    ModuleCompositeRepository moduleCompositeRepository(ModuleCompositeRedisConfigurationProperties properties) {
        final Map<String, ReactiveRepository<PayloadWrapper, String>> applicationToSource = properties.getRedis()
                .entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), getReactiveRepository(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new ModuleCompositeRepository(applicationToSource);
    }

    private static ReactiveRepository<PayloadWrapper, String> getReactiveRepository(
            RedisConfigurationProperties properties) {

        final RedisStringReactiveCommands<String, String> reactiveCommands = properties.getHost() != null
                ? getRedisConnection(properties).reactive()
                : getClusterRedisConnection(properties).reactive();

        return new RedisRepositoryImpl(reactiveCommands);
    }

    private static StatefulRedisClusterConnection<String, String> getClusterRedisConnection(
            RedisConfigurationProperties properties) {

        final RedisClusterClient redisClusterClient = RedisClusterClient.create(RedisUtils.createRedisClusterURIs(
                properties.getCluster(), properties.getTimeout(), properties.getPassword()));
        redisClusterClient.setOptions(RedisUtils.createRedisClusterOptions(properties.getCluster()));

        return redisClusterClient.connect();
    }

    private static StatefulRedisConnection<String, String> getRedisConnection(RedisConfigurationProperties properties) {
        return RedisClient.create(RedisUtils.createRedisURI(
                        properties.getHost(), properties.getPort(), properties.getTimeout(), properties.getPassword()))
                .connect();
    }

}
