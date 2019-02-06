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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConditionalOnProperty(prefix = "spring.redis", name = {"host"})
@ConfigurationProperties(prefix = "spring.redis")
public class RedisPropertyConfiguration {

    private String host;
    private long timeout;
    private String password;
    private Boolean isCluster;

    private RedisURI createRedisURI(String hosts) {
        requireNonNull(hosts);
        String[] hostParams = hosts.split(":");

        if (hostParams.length < 2) {
            throw new IllegalArgumentException("Illegal host URL format, host and port should be specified "
                    + "as host:port");
        }
        String hostname = requireNonNull(hostParams[0]);
        int port = Integer.parseInt(hostParams[1]);

        final RedisURI.Builder builder = RedisURI.Builder.redis(hostname, port)
                .withTimeout(Duration.ofMillis(timeout));
        if (password != null) {
            builder.withPassword(password);
        }

        return builder.build();
    }

    private List<RedisURI> createRedisClusterURIs() {
        requireNonNull(host);
        return Arrays.stream(host.split(","))
                .map(this::createRedisURI)
                .collect(Collectors.toList());
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnExpression("'${spring.redis.is_cluster}' == 'false'")
    RedisClient client() {
        return RedisClient.create(createRedisURI(getHost()));
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${spring.redis.is_cluster}' == 'false'")
    StatefulRedisConnection<String, String> connection() {
        return client().connect();
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnExpression("'${spring.redis.is_cluster}' == 'true'")
    RedisClusterClient clusterClient() {
        return RedisClusterClient.create(createRedisClusterURIs());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${spring.redis.is_cluster}' == 'true'")
    StatefulRedisClusterConnection<String, String> clusterConnection() {
        return clusterClient().connect();
    }

    @Bean
    RedisStringReactiveCommands<String, String> reactiveCommands() {
        if (getIsCluster()) {
            return clusterConnection().reactive();
        } else {
            return connection().reactive();
        }
    }
}
