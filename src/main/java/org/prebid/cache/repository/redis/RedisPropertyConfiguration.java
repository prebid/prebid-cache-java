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
import lombok.Singular;
import org.prebid.cache.helpers.ValidateRedisPropertyConditional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@Conditional(ValidateRedisPropertyConditional.class)
public class RedisPropertyConfiguration {

    private String host;
    private long timeout;
    private String password;
    private int port;
    private Cluster cluster;

    @Data
    @ConditionalOnProperty(prefix = "spring.redis", name = {"cluster"})
    public static class Cluster {

        @Singular
        @NotNull
        List<String> nodes;

        boolean enableTopologyRefresh;

        Integer topologyPeriodicRefreshPeriod;
    }

    private RedisURI createRedisURI(String host, int port) {
        requireNonNull(host);
        final RedisURI.Builder builder = RedisURI.Builder.redis(host, port)
                .withTimeout(Duration.ofMillis(timeout));
        if (password != null) {
            builder.withPassword((CharSequence) password);
        }

        return builder.build();
    }

    private List<RedisURI> createRedisClusterURIs() {

        return cluster.getNodes().stream()
                .map(node -> node.split(":"))
                .map(host -> createRedisURI(host[0], Integer.parseInt(host[1])))
                .collect(Collectors.toList());
    }

    private ClusterClientOptions createRedisClusterOptions() {
        final ClusterTopologyRefreshOptions topologyRefreshOptions = cluster.isEnableTopologyRefresh()
                ? ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh()
                .refreshPeriod(Duration.of(cluster.getTopologyPeriodicRefreshPeriod(), ChronoUnit.SECONDS))
                .enableAllAdaptiveRefreshTriggers()
                .build()
                : null;

        return ClusterClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .topologyRefreshOptions(topologyRefreshOptions)
                .build();
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
        final RedisClusterClient redisClusterClient = RedisClusterClient.create(createRedisClusterURIs());
        redisClusterClient.setOptions(createRedisClusterOptions());

        return redisClusterClient;
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
