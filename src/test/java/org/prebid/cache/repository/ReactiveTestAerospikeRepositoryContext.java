package org.prebid.cache.repository;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.async.EventPolicy;
import com.aerospike.client.async.NettyEventLoops;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.Policy;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.aerospike.AerospikePropertyConfiguration;
import org.prebid.cache.repository.aerospike.AerospikeRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ReactiveTestAerospikeRepositoryContext {

    @Bean
    @Primary
    public ReactiveRepository<PayloadWrapper, String> createRepository() {
        return new AerospikeRepositoryImpl(aerospikePropertyConfiguration(), client(), eventLoops(), readPolicy());
    }

    @Bean
    AerospikePropertyConfiguration aerospikePropertyConfiguration() {
        return new AerospikePropertyConfiguration();
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
        return new NioEventLoopGroup(4);
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

    @Bean
    AerospikeClient client() {
        return new AerospikeClient(clientPolicy(), "localhost", 3000);
    }
}
