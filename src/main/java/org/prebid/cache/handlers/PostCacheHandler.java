package org.prebid.cache.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.ExpiryOutOfRangeException;
import org.prebid.cache.exceptions.InvalidUUIDException;
import org.prebid.cache.exceptions.RequestBodyDeserializeException;
import org.prebid.cache.helpers.RandomUUID;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadTransfer;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.model.RequestObject;
import org.prebid.cache.model.ResponseObject;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@Slf4j
public class PostCacheHandler extends CacheHandler {

    private static final String UUID_KEY = "uuid";
    private static final String SECONDARY_CACHE_KEY = "secondaryCache";

    private final ReactiveRepository<PayloadWrapper, String> repository;
    private final CacheConfig config;
    private final Supplier<Date> currentDateProvider;
    private final Function<PayloadWrapper, Map<String, String>> payloadWrapperToMapTransformer = payload ->
            ImmutableMap.of(UUID_KEY, payload.getId());
    private final Map<String, WebClient> webClients = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CircuitBreaker circuitBreaker;

    @Autowired
    public PostCacheHandler(final ReactiveRepository<PayloadWrapper, String> repository,
                            final CacheConfig config,
                            final MetricsRecorder metricsRecorder,
                            final PrebidServerResponseBuilder builder,
                            final Supplier<Date> currentDateProvider,
                            final CircuitBreaker circuitBreaker) {
        this.metricsRecorder = metricsRecorder;
        this.type = ServiceType.SAVE;
        this.repository = repository;
        this.config = config;
        if (config.getSecondaryUris() != null) {
            config.getSecondaryUris().forEach(ip -> webClients.put(ip, WebClient.create(ip)));
        }
        this.builder = builder;
        this.currentDateProvider = currentDateProvider;
        this.metricTagPrefix = "write";
        this.circuitBreaker = circuitBreaker;
    }

    public Mono<ServerResponse> save(final ServerRequest request) {
        metricsRecorder.markMeterForTag(this.metricTagPrefix, MetricsRecorder.MeasurementTag.REQUEST);
        val timerContext = metricsRecorder.createRequestContextTimerOptionalForServiceType(type)
                .orElse(null);

        String secondaryCache = request.queryParam(SECONDARY_CACHE_KEY).orElse(StringUtils.EMPTY);

        val bodyMono = getRequestBodyMono(request);
        val monoList = bodyMono.map(RequestObject::getPuts);
        val flux = monoList.flatMapMany(Flux::fromIterable);
        val payloadFlux = flux
                .map(payload -> payload.toBuilder()
                        .prefix(config.getPrefix())
                        .expiry(adjustExpiry(payload.compareAndGetExpiry()))
                        .build())
                .map(payloadWrapperTransformer(currentDateProvider))
                .handle(this::validateUUID)
                .handle(this::validateExpiry)
                .concatMap(repository::save)
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .subscribeOn(Schedulers.parallel())
                .collectList()
                .doOnNext(payloadWrappers -> sendRequestToSecondaryPrebidCacheHosts(payloadWrappers, secondaryCache))
                .flatMapMany(Flux::fromIterable)
                .transform(CircuitBreakerOperator.of(circuitBreaker))
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
                        return builder.createResponseMono(request, MediaType.APPLICATION_JSON_UTF8, response);
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
                        currentDateProvider.get(),
                        RandomUUID.isExternalUUID(transfer)
                );
    }

    private void validateUUID(final PayloadWrapper payload, final SynchronousSink<PayloadWrapper> sink) {
        if (payload.isExternalId() && !config.isAllowExternalUUID()) {
            sink.error(new InvalidUUIDException("Prebid cache host forbids specifying UUID in request."));
            return;
        }
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
        if (expiry == null) {
            return config.getExpirySec();
        } else if (expiry > config.getMaxExpiry()) {
            return config.getMaxExpiry();
        } else if (expiry < config.getMinExpiry()) {
            return config.getMinExpiry();
        } else {
            return expiry;
        }
    }

    private void sendRequestToSecondaryPrebidCacheHosts(List<PayloadWrapper> payloadWrappers, String secondaryCache) {
        if (!"yes".equals(secondaryCache) && webClients.size() != 0) {
            final List<PayloadTransfer> payloadTransfers = new ArrayList<>();
            for (PayloadWrapper payloadWrapper : payloadWrappers) {
                payloadTransfers.add(wrapperToTransfer(payloadWrapper));
            }
            RequestObject requestObject = new RequestObject(payloadTransfers);
            webClients.forEach((ip, webClient) -> webClient.post()
                    .uri(uriBuilder -> uriBuilder.path(config.getSecondaryCachePath())
                            .queryParam("secondaryCache", "yes").build())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .syncBody(requestObject)
                    .exchange()
                    .doOnError(throwable -> {
                        metricsRecorder.getSecondaryCacheWriteError().mark();
                        log.info("Failed to send request : ", throwable);
                    })
                    .subscribe(clientResponse -> {
                        if (clientResponse.statusCode() != HttpStatus.OK) {
                            metricsRecorder.getSecondaryCacheWriteError().mark();
                            log.debug(clientResponse.statusCode().toString());
                            log.info("Failed to write to remote address : {}", ip);
                        }
                    }));
        }
    }

    private PayloadTransfer wrapperToTransfer(final PayloadWrapper wrapper) {
        return PayloadTransfer.builder().type(wrapper.getPayload().getType())
                .key(wrapper.getId()).value(wrapper.getPayload().getValue()).expiry(wrapper.getExpiry()).build();
    }

    private Mono<RequestObject> getRequestBodyMono(final ServerRequest request) {
        if (MediaType.TEXT_PLAIN.equals(request.headers().contentType().orElse(MediaType.APPLICATION_JSON))) {
            return request.body(BodyExtractors.toMono(String.class)).map(value -> {
                RequestObject requestObject = null;
                try {
                    requestObject = objectMapper.readValue(value, RequestObject.class);
                } catch (IOException e) {
                    log.error("Exception occurred while deserialize request body", e);
                }
                return requestObject;
            }).doOnError(throwable ->
                    Mono.error(new RequestBodyDeserializeException("Exception occurred while deserialize request body",
                            throwable)));
        }
        return request.bodyToMono(RequestObject.class);
    }

}

