package org.prebid.cache.repository.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.async.EventPolicy;
import com.aerospike.client.async.NettyEventLoops;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.Policy;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;
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
    @NotNull
    private String host;
    @NotNull
    private Integer port;

    private String password;
    @NotNull
    private Integer cores;
    @NotNull
    private Long firstBackoff;
    @NotNull
    private Long maxBackoff;
    @NotNull
    private int maxRetry;
    @NotNull
    private String namespace;
    @NotNull
    private boolean preventUUIDDuplication;

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
        return new Policy();
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
