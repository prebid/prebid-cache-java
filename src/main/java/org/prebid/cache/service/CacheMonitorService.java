package org.prebid.cache.service;

import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CacheMonitorService {

    private static final String DEFAULT_CACHE_TTL = "default";
    private static final String MIN_CACHE_TTL = "min";
    private static final String MAX_CACHE_TTL = "max";
    private static final String STATIC_CACHE_TTL = "static";

    private final ReactiveRepository<PayloadWrapper, String> repository;
    private final MetricsRecorder metricsRecorder;
    private final String prefix;

    private final Map<Long, String> expiryBuckets;
    private final Map<Long, PayloadWrapper> monitoredEntities = new ConcurrentHashMap<>();

    public CacheMonitorService(ReactiveRepository<PayloadWrapper, String> repository,
                               MetricsRecorder metricsRecorder,
                               CacheConfig config) {

        this.repository = repository;
        this.metricsRecorder = metricsRecorder;
        this.prefix = config.getPrefix();
        expiryBuckets = resolveExpiryBuckets(config);
    }

    public Mono<Void> poll() {
        return Flux.fromIterable(expiryBuckets.entrySet())
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(entry ->
                        processExpiryBucket(entry.getKey(),
                                entry.getValue(),
                                monitoredEntities.get(entry.getKey()))
                                .doOnNext(wrapper -> monitoredEntities.put(entry.getKey(), wrapper))
                )
                .then();
    }

    private Mono<PayloadWrapper> processExpiryBucket(Long ttl, String bucketName, PayloadWrapper payloadWrapper) {
        if (Objects.isNull(payloadWrapper)) {
            return saveNewWrapper(ttl);
        }

        try {
            final String normalizedId = payloadWrapper.getNormalizedId();
            return repository.findById(normalizedId)
                    .switchIfEmpty(Mono.defer(() -> {
                        final Duration entryLifetime =
                                Duration.between(Instant.ofEpochMilli(payloadWrapper.getTimestamp()), Instant.now());
                        metricsRecorder.recordEntryLifetime(bucketName, entryLifetime);
                        return saveNewWrapper(ttl);
                    }));
        } catch (PayloadWrapperPropertyException e) {
            return Mono.error(new RuntimeException(e));
        }
    }

    private Mono<PayloadWrapper> saveNewWrapper(Long ttl) {
        final PayloadWrapper newWrapper = PayloadWrapper.builder()
                .id(UUID.randomUUID().toString())
                .prefix(prefix)
                .timestamp(Instant.now().toEpochMilli())
                .expiry(ttl)
                .build();
        return repository.save(newWrapper)
                .doOnSuccess(wrapper -> monitoredEntities.put(ttl, wrapper));
    }

    private static Map<Long, String> resolveExpiryBuckets(CacheConfig config) {
        if (config.getMinExpiry() == config.getMaxExpiry() && config.getMinExpiry() == 0
                || config.getMinExpiry() == config.getMaxExpiry() && config.getMinExpiry() == config.getExpirySec()) {
            return Map.of(config.getExpirySec(), DEFAULT_CACHE_TTL);
        } else if (config.getMinExpiry() == config.getMaxExpiry()) {
            return Map.of(
                    config.getExpirySec(), DEFAULT_CACHE_TTL,
                    config.getMinExpiry(), STATIC_CACHE_TTL);
        } else {
            return Map.of(
                    config.getExpirySec(), DEFAULT_CACHE_TTL,
                    config.getMinExpiry(), MIN_CACHE_TTL,
                    config.getMaxExpiry(), MAX_CACHE_TTL);
        }
    }
}
