package org.prebid.cache.handlers;

import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.util.Strings;
import org.luaj.vm2.ast.Str;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import javax.ws.rs.core.UriBuilder;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;


import static org.springframework.http.MediaType.*;

@Component
@Slf4j
public class PostCacheHandler extends CacheHandler {

    private static final String UUID_KEY = "uuid";
    private final String CACHE_PATH = "cache";
    private final String SECONDARY_CACHE_KEY = "secondaryCache";

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
        this.metricTagPrefix = "write";
    }

    public Mono<ServerResponse> save(final ServerRequest request) {
        metricsRecorder.markMeterForTag(this.metricTagPrefix, MetricsRecorder.MeasurementTag.REQUEST);
        val timerContext = metricsRecorder.createRequestContextTimerOptionalForServiceType(type)
                .orElse(null);

        val bodyMono = request.bodyToMono(RequestObject.class)
                .doOnSuccess(requestObject ->
                        sendRequestToSecondaryPrebidCacheHosts(requestObject, request.queryParam(SECONDARY_CACHE_KEY)
                                                                                     .orElse(Strings.EMPTY)));
        val monoList = bodyMono.map(RequestObject::getPuts);
        val flux = monoList.flatMapMany(Flux::fromIterable);
        val payloadFlux = flux.map(payload -> payload.toBuilder()
                .prefix(config.getPrefix())
                .expiry(adjustExpiry(payload.getExpiry()))
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
                        new Payload(transfer.getType(), transfer.getKey(), transfer.valueAsString()),
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
        }

        sink.next(payload);
    }

    private long adjustExpiry(Long expiry) {
        if(expiry == null) {
            return config.getExpirySec();
        } else if(expiry > config.getMaxExpiry()) {
            return config.getMaxExpiry();
        } else if(expiry < config.getMinExpiry()) {
            return config.getMinExpiry();
        } else {
            return expiry;
        }
    }

    private void sendRequestToSecondaryPrebidCacheHosts(RequestObject requestObject, String secondaryCache) {
        if(!secondaryCache.equals("yes")) {
            config.getSecondaryIps().forEach(ip -> {
                WebClient.create().post().uri(uriBuilder -> uriBuilder.scheme(config.getSecondaryCacheScheme())
                        .host(ip).port(config.getSecondaryCachePort()).path(CACHE_PATH)
                        .queryParam("secondaryCache", "yes").build())
                        .syncBody(requestObject)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .exchange()
                        .doOnError(throwable -> {
                            metricsRecorder.getSecondaryCacheWriteError().mark();
                            log.info("Failed to send request : ", throwable);
                        })
                        .subscribe((clientResponse) -> {
                            if(clientResponse.statusCode() != HttpStatus.OK) {
                                metricsRecorder.getSecondaryCacheWriteError().mark();
                                log.info("Failed to write to {}", ip);
                            }
                        });
            });
        }
    }
}

