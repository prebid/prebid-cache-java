package org.prebid.cache.handlers;

import com.google.common.collect.ImmutableMap;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.ExpiryOutOfRangeException;
import org.prebid.cache.exceptions.InvalidUUIDException;
import org.prebid.cache.metrics.GraphiteMetricsRecorder;
import org.prebid.cache.model.*;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.prebid.cache.helpers.RandomUUID;
import org.prebid.cache.metrics.MetricsRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;


import static org.springframework.http.MediaType.*;

@Component
@Slf4j
public class PostCacheHandler extends CacheHandler {

    private static final String UUID_KEY = "uuid";
    private static final long MIN_EXPIRY = 60;
    private static final long MAX_EXPIRY = 28800;

    private final ReactiveRepository<PayloadWrapper, String> repository;
    private final CacheConfig config;
    private final Supplier<Date> currentDateProvider;
    private final Function<PayloadWrapper, Map<String, String>> payloadWrapperToMapTransformer = payload ->
            ImmutableMap.of(UUID_KEY, payload.getId());

    @Autowired
    public PostCacheHandler(final ReactiveRepository<PayloadWrapper, String> repository,
                            final CacheConfig config,
                            final GraphiteMetricsRecorder metricsRecorder,
                            final PrebidServerResponseBuilder builder,
                            final Supplier<Date> currentDateProvider) {
        this.metricsRecorder = metricsRecorder;
        this.type = ServiceType.SAVE;
        this.repository = repository;
        this.config = config;
        this.builder = builder;
        this.currentDateProvider = currentDateProvider;
    }

    public Mono<ServerResponse> save(final ServerRequest request) {
        metricsRecorder.markMeterForClass(this.getClass(), MetricsRecorder.MeasurementTag.REQUEST_RATE);
        val timerContext = metricsRecorder.createRequestContextTimerOptionalForServiceType(type)
                .orElse(null);
        val bodyMono = request.bodyToMono(RequestObject.class);
        val monoList = bodyMono.map(RequestObject::getPuts);
        val flux = monoList.flatMapMany(Flux::fromIterable);
        val payloadFlux = flux.map(payload -> payload.toBuilder()
                .prefix(config.getPrefix())
                .expiry(payload.getExpiry() == null ? config.getExpirySec() : payload.getExpiry())
                .build())
                .map(payloadWrapperTransformer(currentDateProvider))
                .handle(this::validateUUID)
                .handle(this::validateExpiry)
                .concatMap(repository::save)
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .subscribeOn(Schedulers.parallel());

        final Mono<ServerResponse> responseMono = payloadFlux
                .map(payloadWrapperToMapTransformer)
                .collectList()
                .transform(this::validateErrorResult)
                .map(ResponseObject::new)
                .flatMap(response -> {
                    if (response.getResponses().isEmpty()) {
                        return ErrorHandler.createNoElementsFound();
                    } else {
                        return builder.createResponseMono(request, APPLICATION_JSON_UTF8, response);
                    }
                });

        return finalizeResult(responseMono, request, timerContext);
    }

    private Function<PayloadTransfer, PayloadWrapper> payloadWrapperTransformer(Supplier<Date> currentDateProvider) {
        return transfer ->
                new PayloadWrapper(
                        RandomUUID.extractUUID(transfer),
                        transfer.getPrefix(),
                        new Payload(transfer.getType(), transfer.getKey(), transfer.getValue()),
                        transfer.getExpiry(),
                        currentDateProvider.get()
                );
    }

    private void validateUUID(final PayloadWrapper payload, final SynchronousSink<PayloadWrapper> sink) {
        if (RandomUUID.isValidUUID(payload.getId())) {
            sink.next(payload);
        } else {
            sink.error(new InvalidUUIDException("Invalid UUID: [" + payload.getId() + "]."));
        }
    }

    private void validateExpiry(final PayloadWrapper payload, final SynchronousSink<PayloadWrapper> sink) {
        if (payload.getExpiry() == null) {
            sink.error(new ExpiryOutOfRangeException("Invalid Expiry [NULL]."));
        } else {
            if (payload.getExpiry() > MAX_EXPIRY || payload.getExpiry() < MIN_EXPIRY) {
                sink.error(new ExpiryOutOfRangeException("Invalid Expiry [" + payload.getExpiry() + "]."));
            } else {
                sink.next(payload);
            }
        }
    }
}

