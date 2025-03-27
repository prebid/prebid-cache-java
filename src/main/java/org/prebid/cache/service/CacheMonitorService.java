package org.prebid.cache.service;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class CacheMonitorService {
    private final ReactiveRepository<PayloadWrapper, String> repository;
    private final MetricsRecorder metricsRecorder;
    private final String prefix;

    private final Map<Long, String> monitoredExpiryTimes;
    private final Map<Long, PayloadWrapper> monitoredEntities = new ConcurrentHashMap<>();

    public CacheMonitorService(ReactiveRepository<PayloadWrapper, String> repository,
                               MetricsRecorder metricsRecorder,
                               CacheConfig config) {

        this.repository = repository;
        this.metricsRecorder = metricsRecorder;
        this.prefix = config.getPrefix();
        monitoredExpiryTimes = Map.of(
                config.getMinExpiry(), "min",
                config.getExpirySec(), "default",
                config.getMaxExpiry(), "max");
    }

    public void poll() {
        Flux.fromIterable(monitoredExpiryTimes.entrySet())
                .flatMap(entry ->
                        Mono.defer(() ->
                                processExpiryBucket(entry.getKey(), entry.getValue(), monitoredEntities.get(entry.getKey()))
                                        .doOnNext(wrapper -> monitoredEntities.put(entry.getKey(), wrapper))
                        ))
                .subscribeOn(Schedulers.parallel())
                .doOnError(error -> log.error("Error during cache monitor poll.", error))
                .subscribe();
    }

    private Mono<PayloadWrapper> processExpiryBucket(Long ttl, String bucketName, PayloadWrapper payloadWrapper) {
        if (Objects.isNull(payloadWrapper)) {
            return saveNewWrapper(ttl);
        } else {
            try {
                final String normalizedId = payloadWrapper.getNormalizedId();
                return repository.findById(normalizedId)
                        .switchIfEmpty(Mono.defer(() -> {
                            final Duration entryLifetime = Duration.ofMillis(Instant.now().toEpochMilli() - payloadWrapper.getTimestamp());
                            metricsRecorder.recordEntryLifetime(bucketName, entryLifetime);
                            return saveNewWrapper(ttl);
                        }));
            } catch (PayloadWrapperPropertyException e) {
                return Mono.error(new RuntimeException(e));
            }
        }
    }

    private Mono<PayloadWrapper> saveNewWrapper(Long ttl) {
        final PayloadWrapper wrapper = PayloadWrapper.builder()
                .id(UUID.randomUUID().toString())
                .prefix(prefix)
                .timestamp(Instant.now().toEpochMilli())
                .expiry(ttl)
                .build();
        return repository.save(wrapper);
    }
}
