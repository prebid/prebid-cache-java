package org.prebid.cache.repository.redis;

import io.lettuce.core.RedisException;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.helpers.Json;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@Slf4j
@ConditionalOnProperty(prefix = "spring.redis", name = {"timeout"})
@RequiredArgsConstructor
public class RedisRepositoryImpl implements ReactiveRepository<PayloadWrapper, String> {
    private final RedisStringReactiveCommands<String, String> reactiveCommands;

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
            return reactiveCommands.setex(normalizedId, expiry, Json.toJson(wrapper))
                    .map(payload -> wrapper);
        } catch (RedisException e) {
            return Mono.error(new RepositoryException(e.toString(), e));
        }
    }

    @Override
    public Mono<PayloadWrapper> findById(final String id) {
        try {
            return reactiveCommands.get(id)
                    .map(json -> Json.createPayloadFromJson(json, PayloadWrapper.class));
        } catch (RedisException e) {
            return Mono.error(new RepositoryException(e.toString(), e));
        }
    }
}
