package org.prebid.cache.repository.redis;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.masterslave.MasterSlave;
import io.lettuce.core.masterslave.StatefulRedisMasterSlaveConnection;
import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.helpers.Json;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@Slf4j
@ConditionalOnProperty(prefix = "spring.redis", name = {"host"})
public class RedisRepositoryImpl implements ReactiveRepository<PayloadWrapper, String> {
    private final RedisPropertyConfiguration redisConfig;
    private final RedisSentinelPropertyConfiguration sentinelConfig;
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private StatefulRedisMasterSlaveConnection<String, String> sentinelConnection;

    @Autowired
    public RedisRepositoryImpl(final RedisPropertyConfiguration redisConfig,
                               final RedisSentinelPropertyConfiguration sentinelConfig) {
        this.redisConfig = redisConfig;
        this.sentinelConfig = sentinelConfig;
    }

    @Override
    public Mono<PayloadWrapper> save(final PayloadWrapper wrapper) {
        long expiry;
        String normalizedId;

        try {
            expiry = wrapper.getExpiry();
            normalizedId = wrapper.getNormalizedId();
        } catch (PayloadWrapperPropertyException e) {
            log.error(e.getMessage(), e);
            return Mono.empty();
        }

        try {
            return createReactive().setex(normalizedId, expiry, Json.toJson(wrapper))
                    .map(payload -> wrapper);
        } catch (RedisException e) {
            return Mono.error(new RepositoryException(e.toString(), e));
        }
    }

    @Override
    public Mono<PayloadWrapper> findById(final String id) {
        try {
            return createReactive().get(id)
                    .map(json -> Json.createPayloadFromJson(json, PayloadWrapper.class));
        } catch (RedisException e) {
            return Mono.error(new RepositoryException(e.toString(), e));
        }
    }

    private StatefulRedisConnection<String, String> getConnection() {
        if (connection == null || !connection.isOpen()) {
            connection = getRedisClient().connect();
        }
        return connection;
    }

    private StatefulRedisMasterSlaveConnection<String, String> getSentinelConnection() {
        if (sentinelConnection == null || !sentinelConnection.isOpen()) {
            sentinelConnection = MasterSlave.connect(getRedisClient(), new Utf8StringCodec(),
                    redisConfig.createRedisURI());
            sentinelConnection.setReadFrom(ReadFrom.NEAREST);
        }
        return sentinelConnection;
    }

    private RedisStringReactiveCommands<String, String> createReactive() {
        if (isStandaloneRedis()) {
            return getConnection().reactive();
        } else {
            return getSentinelConnection().reactive();
        }
    }

    private RedisClient getRedisClient() {
        if (client != null)
            return client;

        if (isStandaloneRedis()) {
            client = RedisClient.create(redisConfig.createRedisURI());
        } else {
            RedisURI uri = sentinelConfig.createRedisURI(redisConfig);
            client = RedisClient.create(uri);
        }
        return client;
    }

    private boolean isStandaloneRedis() {
        return sentinelConfig.getMaster() == null;
    }
}
