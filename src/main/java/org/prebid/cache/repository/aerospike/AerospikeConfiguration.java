package org.prebid.cache.repository.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.async.EventPolicy;
import com.aerospike.client.async.NettyEventLoops;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.Policy;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.aerospike", name = {"host"})
public class AerospikeConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.aerospike")
    public AerospikeConfigurationProperties aerospikeConfigurationProperties() {
        return new AerospikeConfigurationProperties();
    }

    @Bean
    Policy readPolicy(AerospikeConfigurationProperties properties) {
        final Policy policy = new Policy();
        policy.setConnectTimeout(properties.getConnectTimeout());
        policy.setTimeouts(properties.getSocketTimeout(), properties.getTotalTimeout());
        policy.setReplica(properties.getReadPolicy());
        return policy;
    }

    @Bean
    EventPolicy eventPolicy() {
        return new EventPolicy();
    }

    @Bean
    EventLoopGroup eventGroup(AerospikeConfigurationProperties properties) {
        return new NioEventLoopGroup(properties.getCores());
    }

    @Bean
    EventLoops eventLoops(EventLoopGroup eventGroup) {
        return new NettyEventLoops(eventPolicy(), eventGroup);
    }

    @Bean(destroyMethod = "close")
    AerospikeClient client(AerospikeConfigurationProperties properties, EventLoops eventLoops) {
        final ClientPolicy clientPolicy = clientPolicy(properties, eventLoops);
        final String host = properties.getHost();
        return AerospikeConfigurationProperties.isAerospikeCluster(host)
                ? new AerospikeClient(clientPolicy, AerospikeConfigurationProperties.extractHosts(host))
                : new AerospikeClient(clientPolicy, host, properties.getPort());
    }

    private ClientPolicy clientPolicy(AerospikeConfigurationProperties properties, EventLoops eventLoops) {
        ClientPolicy clientPolicy = new ClientPolicy();
        clientPolicy.setEventLoops(eventLoops);
        clientPolicy.setMinConnsPerNode(properties.getMinConnsPerNode());
        clientPolicy.setMaxConnsPerNode(properties.getMaxConnsPerNode());
        return clientPolicy;
    }
}
