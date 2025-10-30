package org.prebid.cache.repository.module.storage;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.async.EventPolicy;
import com.aerospike.client.async.NettyEventLoops;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.Policy;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.netty.channel.nio.NioEventLoopGroup;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.repository.aerospike.AerospikeConfigurationProperties;
import org.prebid.cache.repository.aerospike.AerospikeRepositoryImpl;
import org.prebid.cache.repository.redis.RedisConfigurationProperties;
import org.prebid.cache.repository.redis.RedisRepositoryImpl;
import org.prebid.cache.repository.redis.RedisUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class ModuleCompositeRepositoryConfiguration {

    @Bean
    ModuleCompositeRepository moduleCompositeRepository(ModuleCompositeConfigurationProperties properties) {
        final Map<String, ReactiveRepository<PayloadWrapper, String>> allRepositories = Stream.concat(
                properties.getRedis().entrySet().stream()
                        .map(entry -> Map.entry(entry.getKey(), getRedisRepository(entry.getValue()))),
                properties.getAerospike().entrySet().stream()
                        .map(entry -> Map.entry(entry.getKey(), getAerospikeRepository(entry.getValue()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new ModuleCompositeRepository(allRepositories);
    }

    private static ReactiveRepository<PayloadWrapper, String> getRedisRepository(
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

    private static ReactiveRepository<PayloadWrapper, String> getAerospikeRepository(
            AerospikeConfigurationProperties properties) {

        final EventLoops eventLoops = new NettyEventLoops(
                new EventPolicy(),
                new NioEventLoopGroup(properties.getCores()));
        final ClientPolicy clientPolicy = clientPolicy(properties, eventLoops);
        final String host = properties.getHost();
        final AerospikeClient aerospikeClient = AerospikeConfigurationProperties.isAerospikeCluster(host)
                ? new AerospikeClient(clientPolicy, AerospikeConfigurationProperties.extractHosts(host))
                : new AerospikeClient(clientPolicy, host, properties.getPort());

        return new AerospikeRepositoryImpl(properties, aerospikeClient, eventLoops, readPolicy(properties));
    }

    private static ClientPolicy clientPolicy(AerospikeConfigurationProperties properties, EventLoops eventLoops) {
        ClientPolicy clientPolicy = new ClientPolicy();
        clientPolicy.setEventLoops(eventLoops);
        clientPolicy.setMinConnsPerNode(properties.getMinConnsPerNode());
        clientPolicy.setMaxConnsPerNode(properties.getMaxConnsPerNode());
        return clientPolicy;
    }

    private static Policy readPolicy(AerospikeConfigurationProperties properties) {
        final Policy policy = new Policy();
        policy.setConnectTimeout(properties.getConnectTimeout());
        policy.setTimeouts(properties.getSocketTimeout(), properties.getTotalTimeout());
        policy.setReplica(properties.getReadPolicy());
        return policy;
    }

}
