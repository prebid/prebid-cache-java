package org.prebid.cache.repository.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Data
@NoArgsConstructor
@Configuration
@Conditional(RedisConfigurationValidator.class)
public class RedisConfiguration {


    @Bean
    @ConfigurationProperties(prefix = "spring.redis")
    public RedisConfigurationProperties redisConfigurationProperties() {
        return new RedisConfigurationProperties();
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "spring.redis", name = "host")
    RedisClient client(RedisConfigurationProperties properties) {
        return RedisClient.create(RedisUtils.createRedisURI(
                properties.getHost(), properties.getPort(), properties.getTimeout(), properties.getPassword()));
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "spring.redis", name = "host")
    StatefulRedisConnection<String, String> connection(RedisClient client) {
        return client.connect();
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.redis", name = "host")
    RedisStringReactiveCommands<String, String> reactiveCommands(StatefulRedisConnection<String, String> connection) {
        return connection.reactive();
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "spring.redis", name = "host", matchIfMissing = true, havingValue = "null")
    RedisClusterClient clusterClient(RedisConfigurationProperties properties) {
        final RedisClusterClient redisClusterClient = RedisClusterClient.create(RedisUtils.createRedisClusterURIs(
                properties.getCluster(), properties.getTimeout(), properties.getPassword()));
        redisClusterClient.setOptions(RedisUtils.createRedisClusterOptions(properties.getCluster()));

        return redisClusterClient;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "spring.redis", name = "host", matchIfMissing = true, havingValue = "null")
    StatefulRedisClusterConnection<String, String> clusterConnection(RedisClusterClient client) {
        return client.connect();
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.redis", name = "host", matchIfMissing = true, havingValue = "null")
    RedisStringReactiveCommands<String, String> clusterReactiveCommands(
            StatefulRedisClusterConnection<String, String> connection) {

        return connection.reactive();
    }
}
