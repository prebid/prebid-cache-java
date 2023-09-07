package org.prebid.cache.repository.redis;

import io.lettuce.core.RedisException;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.helpers.Json;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import reactor.core.publisher.Mono;

@Slf4j
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
            log.error("Exception occurred while getting payload wrapper property: '{}', cause: '{}'",
                    ExceptionUtils.getMessage(e), ExceptionUtils.getMessage(e));
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
