package org.prebid.cache.repository.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConditionalOnProperty(prefix = "spring.redis", name = {"timeout"})
@ConfigurationProperties(prefix = "spring.redis")
public class RedisPropertyConfiguration {

    private String host;
    private long timeout;
    private String password;
    private int port;
    private Cluster cluster;

    @Data
    public static class Cluster {

        @Singular
        List<String> nodes;
    }

    private RedisURI createRedisURI(String host, int port) {
        requireNonNull(host);
        final RedisURI.Builder builder = RedisURI.Builder.redis(host, port)
                .withTimeout(Duration.ofMillis(timeout));
        if (password != null) {
            builder.withPassword(password);
        }

        return builder.build();
    }

    private List<RedisURI> createRedisClusterURIs() {

        return cluster.getNodes().stream()
                .map(node -> node.split(":"))
                .map(host -> createRedisURI(host[0], Integer.parseInt(host[1])))
                .collect(Collectors.toList());
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "spring.redis", name = "host")
    RedisClient client() {
        return RedisClient.create(createRedisURI(getHost(), getPort()));
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "spring.redis", name = "host")
    StatefulRedisConnection<String, String> connection() {
        return client().connect();
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "spring.redis", name = "host", matchIfMissing = true, havingValue = "null")
    RedisClusterClient clusterClient() {
        return RedisClusterClient.create(createRedisClusterURIs());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "spring.redis", name = "host", matchIfMissing = true, havingValue = "null")
    StatefulRedisClusterConnection<String, String> clusterConnection() {
        return clusterClient().connect();
    }

    @Bean
    RedisStringReactiveCommands<String, String> reactiveCommands() {
        if (getHost() == null) {
            return clusterConnection().reactive();
        } else {
            return connection().reactive();
        }
    }
}
