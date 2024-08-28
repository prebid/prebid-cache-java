package org.prebid.cache.handlers.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.handlers.ErrorHandler;
import org.prebid.cache.handlers.PayloadType;
import org.prebid.cache.handlers.ServiceType;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.MetricsRecorder.MetricsRecorderTimer;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class GetCacheHandler {

    private static final ServiceType SERVICE_TYPE = ServiceType.FETCH;
    private static final String METRIC_TAG_PREFIX = "read";

    private final CacheConfig config;
    private final ApiConfig apiConfig;
    private final CacheHandler cacheHandler;
    private final Map<String, WebClient> clientsCache;
    private final ReactiveRepository<PayloadWrapper, String> repository;
    private final CircuitBreaker circuitBreaker;
    private final PrebidServerResponseBuilder builder;
    private final MetricsRecorder metricsRecorder;

    @Autowired
    public GetCacheHandler(CacheConfig config,
                           ApiConfig apiConfig,
                           CacheHandler cacheHandler,
                           ReactiveRepository<PayloadWrapper, String> repository,
                           CircuitBreaker webClientCircuitBreaker,
                           PrebidServerResponseBuilder builder,
                           MetricsRecorder metricsRecorder) {

        this.config = Objects.requireNonNull(config);
        this.apiConfig = Objects.requireNonNull(apiConfig);
        this.cacheHandler = Objects.requireNonNull(cacheHandler);
        this.clientsCache = createClientsCache(config.getClientsCacheDuration(), config.getClientsCacheSize());
        this.repository = Objects.requireNonNull(repository);
        this.circuitBreaker = Objects.requireNonNull(webClientCircuitBreaker);
        this.builder = Objects.requireNonNull(builder);
        this.metricsRecorder = Objects.requireNonNull(metricsRecorder);
    }

    private static Map<String, WebClient> createClientsCache(int ttl, int size) {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttl, TimeUnit.SECONDS)
                .maximumSize(size)
                .<String, WebClient>build()
                .asMap();
    }

    public Mono<ServerResponse> fetch(ServerRequest request) {
        // metrics
        final MetricsRecorder.MetricsRecorderTimer timerContext =
                cacheHandler.timerContext(SERVICE_TYPE, METRIC_TAG_PREFIX);

        return request.queryParam(CacheHandler.ID_KEY)
                .map(id -> fetch(request, id, timerContext))
                .orElseGet(() -> cacheHandler.finalizeResult(
                        ErrorHandler.createInvalidParameters(),
                        request,
                        timerContext,
                        METRIC_TAG_PREFIX));
    }

    private Mono<ServerResponse> fetch(ServerRequest request, String id, MetricsRecorderTimer timerContext) {
        final String cacheUrl = resolveCacheUrl(request);
        final Mono<ServerResponse> responseMono = StringUtils.containsIgnoreCase(cacheUrl, config.getAllowedProxyHost())
                ? processProxyRequest(request, id, cacheUrl)
                : processRequest(request, id);

        return cacheHandler.finalizeResult(responseMono, request, timerContext, METRIC_TAG_PREFIX);
    }

    private String resolveCacheUrl(ServerRequest request) {
        return request.queryParam(CacheHandler.CACHE_HOST_KEY)
                .filter(StringUtils::isNotBlank)
                .map(cacheHostParam -> new URIBuilder()
                        .setHost(cacheHostParam)
                        .setPath(apiConfig.getCachePath())
                        .setScheme(config.getHostParamProtocol())
                        .toString())
                .orElse(null);
    }

    private Mono<ServerResponse> processProxyRequest(ServerRequest request, String idKeyParam, String cacheUrl) {
        return clientsCache.computeIfAbsent(cacheUrl, WebClient::create).get()
                .uri(uriBuilder -> uriBuilder.queryParam(CacheHandler.ID_KEY, idKeyParam).build())
                .headers(httpHeaders -> httpHeaders.addAll(request.headers().asHttpHeaders()))
                .exchange()
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .subscribeOn(Schedulers.parallel())
                .handle(this::updateProxyMetrics)
                .flatMap(GetCacheHandler::fromClientResponse)
                .doOnError(this::handleProxyRequestError);
    }

    private void updateProxyMetrics(ClientResponse clientResponse, SynchronousSink<ClientResponse> sink) {
        if (HttpStatus.OK.equals(clientResponse.statusCode())) {
            metricsRecorder.getProxySuccess().increment();
        } else {
            metricsRecorder.getProxyFailure().increment();
        }

        sink.next(clientResponse);
    }

    private static Mono<ServerResponse> fromClientResponse(ClientResponse clientResponse) {
        return ServerResponse.status(clientResponse.statusCode())
                .headers(headerConsumer -> clientResponse.headers().asHttpHeaders().forEach(headerConsumer::addAll))
                .body(clientResponse.bodyToMono(String.class), String.class);
    }

    private void handleProxyRequestError(Throwable error) {
        metricsRecorder.getProxyFailure().increment();
        log.info(
                "Failed to send request: '{}', cause: '{}'",
                ExceptionUtils.getMessage(error),
                ExceptionUtils.getMessage(error));
    }

    private Mono<ServerResponse> processRequest(ServerRequest request, String keyIdParam) {
        final String normalizedId = String.format("%s%s", config.getPrefix(), keyIdParam);
        return repository.findById(normalizedId)
                .subscribeOn(Schedulers.parallel())
                .transform(mono -> cacheHandler.validateErrorResult(SERVICE_TYPE, mono))
                .flatMap(wrapper -> createServerResponse(wrapper, request))
                .switchIfEmpty(ErrorHandler.createResourceNotFound(normalizedId));
    }

    private Mono<ServerResponse> createServerResponse(PayloadWrapper wrapper, ServerRequest request) {
        final String payloadType = wrapper.getPayload().getType();
        if (payloadType.equals(PayloadType.JSON.toString())) {
            metricsRecorder.markMeterForTag(METRIC_TAG_PREFIX, MetricsRecorder.MeasurementTag.JSON);
            return builder.createResponseMono(request, MediaType.APPLICATION_JSON_UTF8, wrapper);
        } else if (payloadType.equals(PayloadType.XML.toString())) {
            metricsRecorder.markMeterForTag(METRIC_TAG_PREFIX, MetricsRecorder.MeasurementTag.XML);
            return builder.createResponseMono(request, MediaType.APPLICATION_XML, wrapper);
        }

        return Mono.error(new UnsupportedMediaTypeException("Unsupported Media Type."));
    }
}

