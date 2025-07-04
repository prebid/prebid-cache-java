package org.prebid.cache.repository.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.async.EventPolicy;
import com.aerospike.client.async.NettyEventLoops;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.Replica;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(prefix = "spring.aerospike", name = {"host"})
@ConfigurationProperties(prefix = "spring.aerospike")
public class AerospikePropertyConfiguration {
    private String host;
    private Integer port;

    private String password;
    private Integer cores;
    private Long firstBackoff;
    private Long maxBackoff;
    private int maxRetry;
    private String namespace;
    private boolean preventUUIDDuplication;
    private int socketTimeout;
    private int totalTimeout;
    private int connectTimeout;
    private int minConnsPerNode;
    private int maxConnsPerNode;
    private Replica readPolicy;

    private static final int DEFAULT_PORT = 3000;

    public static Host[] extractHosts(@NotNull String hostList) {
        return Arrays.stream(hostList.split(","))
                .map(host -> {
                    String[] params = host.split(":");
                    String hostname = requireNonNull(params[0]);
                    int port = DEFAULT_PORT;
                    if (params.length == 2) {
                        port = Integer.parseInt(params[1]);
                    }
                    return new Host(hostname, port);
                })
                .toArray(Host[]::new);
    }

    public static boolean isAerospikeCluster(@NotNull String hostList) {
        return hostList.split(",").length > 1;
    }

    @Bean
    Policy readPolicy() {
        final Policy policy = new Policy();
        policy.setConnectTimeout(connectTimeout);
        policy.setTimeouts(socketTimeout, totalTimeout);
        policy.setReplica(readPolicy);
        return policy;
    }

    @Bean
    EventPolicy eventPolicy() {
        return new EventPolicy();
    }

    @Bean
    EventLoopGroup eventGroup() {
        return new NioEventLoopGroup(cores);
    }

    @Bean
    EventLoops eventLoops() {
        return new NettyEventLoops(eventPolicy(), eventGroup());
    }

    @Bean
    ClientPolicy clientPolicy() {
        ClientPolicy clientPolicy = new ClientPolicy();
        clientPolicy.eventLoops = eventLoops();
        clientPolicy.minConnsPerNode = minConnsPerNode;
        clientPolicy.maxConnsPerNode = maxConnsPerNode;
        return clientPolicy;
    }

    @Bean(destroyMethod = "close")
    AerospikeClient client() {
        if (isAerospikeCluster(getHost())) {
            return new AerospikeClient(clientPolicy(), extractHosts(getHost()));
        } else {
            return new AerospikeClient(clientPolicy(), getHost(), getPort());
        }
    }

}
