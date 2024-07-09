package org.prebid.cache.repository.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class RedisUtils {

    private RedisUtils() {
    }

    public static RedisURI createRedisURI(String host, int port, long timeout, String password) {
        requireNonNull(host);
        final RedisURI.Builder builder = RedisURI.Builder.redis(host, port)
                .withTimeout(Duration.ofMillis(timeout));
        if (password != null) {
            builder.withPassword((CharSequence) password);
        }

        return builder.build();
    }

    public static List<RedisURI> createRedisClusterURIs(RedisConfigurationProperties.Cluster cluster,
                                                        long timeout,
                                                        String password) {

        return cluster.getNodes().stream()
                .map(node -> node.split(":"))
                .map(host -> createRedisURI(host[0], Integer.parseInt(host[1]), timeout, password))
                .collect(Collectors.toList());
    }

    public static ClusterClientOptions createRedisClusterOptions(RedisConfigurationProperties.Cluster cluster) {
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

}
