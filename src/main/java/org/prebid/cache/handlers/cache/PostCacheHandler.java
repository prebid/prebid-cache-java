package org.prebid.cache.handlers.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.InvalidUUIDException;
import org.prebid.cache.handlers.ErrorHandler;
import org.prebid.cache.handlers.ServiceType;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadTransfer;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.model.RequestObject;
import org.prebid.cache.model.ResponseObject;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PostCacheHandler {

    private static final ServiceType SERVICE_TYPE = ServiceType.SAVE;
    private static final String METRIC_TAG_PREFIX = "write";
    private static final Pattern UUID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]*$");
    private static final String SECONDARY_CACHE_KEY = "secondaryCache";
    private static final String SECONDARY_CACHE_VALUE = "yes";

    private final CacheConfig config;
    private final CacheHandler cacheHandler;
    private final Map<String, WebClient> webClients;
    private final ReactiveRepository<PayloadWrapper, String> repository;
    private final CircuitBreaker circuitBreaker;
    private final PrebidServerResponseBuilder builder;
    private final MetricsRecorder metricsRecorder;
    private final ObjectMapper mapper;

    @Autowired
    public PostCacheHandler(CacheConfig config,
                            CacheHandler cacheHandler,
                            ReactiveRepository<PayloadWrapper, String> repository,
                            CircuitBreaker webClientCircuitBreaker,
                            PrebidServerResponseBuilder builder,
                            MetricsRecorder metricsRecorder,
                            ObjectMapper objectMapper) {

        this.config = Objects.requireNonNull(config);
        this.cacheHandler = Objects.requireNonNull(cacheHandler);
        this.webClients = createWebClients(config);
        this.repository = Objects.requireNonNull(repository);
        this.circuitBreaker = Objects.requireNonNull(webClientCircuitBreaker);
        this.builder = Objects.requireNonNull(builder);
        this.metricsRecorder = Objects.requireNonNull(metricsRecorder);
        this.mapper = Objects.requireNonNull(objectMapper);
    }

    private static Map<String, WebClient> createWebClients(CacheConfig config) {
        final Map<String, WebClient> webClients = new HashMap<>();
        final List<String> secondaryUris = config.getSecondaryUris();
        if (secondaryUris == null || secondaryUris.isEmpty()) {
            return Collections.unmodifiableMap(webClients);
        }

        for (String ip : secondaryUris) {
            final String url = new URIBuilder()
                    /*
                     * TODO: scheme?
                     * Previous version:
                     *  - WebClient.create(ip)
                     *  - webClient.post()
                            .uri(uriBuilder -> uriBuilder.path(config.getSecondaryCachePath())
                            .queryParam("secondaryCache", "yes").build())
                     */
                    .setScheme(config.getHostParamProtocol())
                    .setHost(ip)
                    .setPath(config.getSecondaryCachePath())
                    .addParameter(SECONDARY_CACHE_KEY, SECONDARY_CACHE_VALUE)
                    .toString();

            webClients.put(ip, WebClient.create(url));
        }

        return Collections.unmodifiableMap(webClients);
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        final MetricsRecorder.MetricsRecorderTimer timerContext =
                cacheHandler.timerContext(SERVICE_TYPE, METRIC_TAG_PREFIX);

        final String secondaryCache = request.queryParam(SECONDARY_CACHE_KEY).orElse(StringUtils.EMPTY);

        final Mono<ServerResponse> responseMono = getRequestBodyMono(request)
                .map(RequestObject::getPuts)
                .flatMapMany(Flux::fromIterable)
                .map(payload -> payload.toBuilder()
                        .prefix(config.getPrefix())
                        .expiry(adjustExpiry(payload.compareAndGetExpiry()))
                        .build())
                .map(payloadWrapperTransformer())
                .handle(this::validateUUID)

                .concatMap(repository::save)
                .subscribeOn(Schedulers.parallel())
                .collectList()
                .doOnNext(payloadWrappers -> sendRequestToSecondaryPrebidCacheHosts(payloadWrappers, secondaryCache))

                .flatMapMany(Flux::fromIterable)
                .subscribeOn(Schedulers.parallel())
                .map(payload -> (Map<String, String>) ImmutableMap.of(CacheHandler.ID_KEY, payload.getId()))
                .collectList()
                .transform(mono -> cacheHandler.validateErrorResult(SERVICE_TYPE, mono))
                .map(ResponseObject::of)
                .flatMap(response -> !response.getResponses().isEmpty()
                        ? builder.createResponseMono(request, MediaType.APPLICATION_JSON_UTF8, response)
                        : ErrorHandler.createNoElementsFound());

        return cacheHandler.finalizeResult(responseMono, request, timerContext, METRIC_TAG_PREFIX);
    }

    private Mono<RequestObject> getRequestBodyMono(ServerRequest request) {
        final MediaType mediaType = request.headers().contentType().orElse(null);
        if (!MediaType.TEXT_PLAIN.equals(mediaType)) {
            return request.bodyToMono(RequestObject.class);
        }

        return request.body(BodyExtractors.toMono(String.class))
                .mapNotNull(this::readRequestObject);
    }

    private RequestObject readRequestObject(String body) {
        try {
            return mapper.readValue(body, RequestObject.class);
        } catch (IOException e) {
            log.error(
                    "Exception occurred while deserialize request body: '{}', cause: '{}'",
                    ExceptionUtils.getMessage(e),
                    ExceptionUtils.getMessage(e));
            return null;
        }
    }

    private long adjustExpiry(Long expiry) {
        return expiry != null
                ? Math.min(Math.max(expiry, config.getMinExpiry()), config.getMaxExpiry())
                : config.getExpirySec();
    }

    private Function<PayloadTransfer, PayloadWrapper> payloadWrapperTransformer() {
        return transfer -> PayloadWrapper.builder()
                .id(uuidFrom(transfer))
                .prefix(transfer.getPrefix())
                .payload(Payload.of(transfer.getType(), transfer.getKey(), transfer.valueAsString()))
                .expiry(transfer.getExpiry())
                .isExternalId(transfer.getKey() != null)
                .build();
    }

    private static String uuidFrom(PayloadTransfer payload) {
        return payload.getKey() != null ? payload.getKey() : String.valueOf(UUID.randomUUID());
    }

    private void validateUUID(PayloadWrapper payload, SynchronousSink<PayloadWrapper> sink) {
        if (!payload.isExternalId()) {
            sink.next(payload);
            return;
        }

        if (!config.isAllowExternalUUID()) {
            sink.error(new InvalidUUIDException("Prebid cache host forbids specifying UUID in request."));
            return;
        }

        final String uuid = payload.getId();
        if (isValidUUID(uuid)) {
            sink.next(payload);
        } else {
            sink.error(new InvalidUUIDException("Invalid UUID: [" + uuid + "]."));
        }
    }

    private static boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            log.error("UUID cannot be NULL or zero length !!");
            return false;
        }

        final boolean isValid = UUID_PATTERN.matcher(uuid).matches();
        if (!isValid) {
            log.debug("Invalid UUID: {}", uuid);
        }
        return isValid;
    }

    private void sendRequestToSecondaryPrebidCacheHosts(List<PayloadWrapper> payloadWrappers, String secondaryCache) {
        if (webClients.isEmpty() || SECONDARY_CACHE_VALUE.equals(secondaryCache)) {
            return;
        }

        final List<PayloadTransfer> payloadTransfers = payloadWrappers.stream()
                .map(PostCacheHandler::wrapperToTransfer)
                .toList();

        webClients.forEach((ip, webClient) -> webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(RequestObject.of(payloadTransfers))
                .exchange()
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .doOnError(this::handleSecondaryCacheRequestError)
                .subscribe(clientResponse -> handleSecondaryCacheResponse(clientResponse, ip)));
    }

    private static PayloadTransfer wrapperToTransfer(PayloadWrapper wrapper) {
        return PayloadTransfer.builder()
                .type(wrapper.getPayload().getType())
                .key(wrapper.getId())
                .value(wrapper.getPayload().getValue())
                .expiry(wrapper.getExpiry())
                .build();
    }

    private void handleSecondaryCacheRequestError(Throwable throwable) {
        metricsRecorder.getSecondaryCacheWriteError().increment();
        log.info(
                "Failed to send request: '{}', cause: '{}'",
                ExceptionUtils.getMessage(throwable),
                ExceptionUtils.getMessage(throwable));
    }

    private void handleSecondaryCacheResponse(ClientResponse clientResponse, String ip) {
        if (clientResponse.statusCode() != HttpStatus.OK) {
            metricsRecorder.getSecondaryCacheWriteError().increment();
            log.debug(clientResponse.statusCode().toString());
            log.info("Failed to write to remote address : {}", ip);
        }
    }
}
