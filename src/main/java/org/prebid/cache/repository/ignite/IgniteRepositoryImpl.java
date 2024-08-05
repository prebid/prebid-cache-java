package org.prebid.cache.repository.ignite;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientException;
import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.helpers.Json;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import reactor.core.publisher.Mono;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IgniteRepositoryImpl implements ReactiveRepository<PayloadWrapper, String> {

    private final ClientCache<String, String> cache;

    public IgniteRepositoryImpl(ClientCache<String, String> cache) {
        this.cache = cache;
    }

    @Override
    public Mono<PayloadWrapper> save(PayloadWrapper wrapper) {
        ExpiryPolicy expiryPolicy;
        String normalizedId;

        try {
            normalizedId = wrapper.getNormalizedId();
            expiryPolicy = new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS, wrapper.getExpiry()));
        } catch (PayloadWrapperPropertyException e) {
            log.error("Exception occurred while extracting normalized id from payload: '{}', cause: '{}'",
                    ExceptionUtils.getMessage(e), ExceptionUtils.getMessage(e));
            return Mono.empty();
        }

        final ClientCache<String, String> expiredCache = cache.withExpirePolicy(expiryPolicy);
        return Mono.fromFuture(expiredCache.putIfAbsentAsync(normalizedId, Json.toJson(wrapper)).toCompletableFuture())
                .map(payload -> wrapper)
                .onErrorResume(IgniteRepositoryImpl::handleError);
    }

    @Override
    public Mono<PayloadWrapper> findById(String id) {
        return Mono.fromFuture(cache.getAsync(id).toCompletableFuture())
                .map(json -> Json.createPayloadFromJson(json, PayloadWrapper.class))
                .onErrorResume(IgniteRepositoryImpl::handleError);
    }

    private static <T> Mono<T> handleError(Throwable throwable) {
        if (throwable instanceof ClientException) {
            return Mono.error(new RepositoryException(throwable.toString(), throwable));
        }

        return Mono.error(throwable);
    }
}
