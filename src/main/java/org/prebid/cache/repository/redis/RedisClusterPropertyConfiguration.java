package org.prebid.cache.repository.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
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

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConditionalOnProperty(prefix = "spring.redis.cluster", name = {"timeout"})
@ConfigurationProperties(prefix = "spring.redis.cluster")
public class RedisClusterPropertyConfiguration {

    @Singular
    @NotNull
    private List<String> nodes;

    boolean enableTopologyRefresh;

    Integer topologyPeriodicRefreshPeriod;

    String password;

    @NotNull
    long timeout;

    private RedisURI createRedisURI(String host, int port) {
        final RedisURI.Builder builder = RedisURI.Builder.redis(host, port)
                .withTimeout(Duration.ofMillis(timeout));
        if (password != null) {
            builder.withPassword((CharSequence) password);
        }

        return builder.build();
    }

    private List<RedisURI> createRedisClusterURIs() {
        return getNodes().stream()
                .map(node -> node.split(":"))
                .map(host -> createRedisURI(host[0], Integer.parseInt(host[1])))
                .collect(Collectors.toList());
    }

    private ClusterClientOptions createRedisClusterOptions() {
        final ClusterTopologyRefreshOptions topologyRefreshOptions = isEnableTopologyRefresh()
                ? ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh()
                .refreshPeriod(Duration.of(getTopologyPeriodicRefreshPeriod(), ChronoUnit.SECONDS))
                .enableAllAdaptiveRefreshTriggers()
                .build()
                : null;

        return ClusterClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .topologyRefreshOptions(topologyRefreshOptions)
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    RedisClusterClient clusterClient() {
        final RedisClusterClient redisClusterClient = RedisClusterClient.create(createRedisClusterURIs());
        redisClusterClient.setOptions(createRedisClusterOptions());

        return redisClusterClient;
    }

    @Bean(destroyMethod = "close")
    StatefulRedisClusterConnection<String, String> clusterConnection() {
        return clusterClient().connect();
    }

    @Bean
    RedisStringReactiveCommands<String, String> reactiveCommands() {
        return clusterConnection().reactive();
    }
}
