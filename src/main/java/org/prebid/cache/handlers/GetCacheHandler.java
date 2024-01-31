package org.prebid.cache.handlers;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.MetricsRecorder.RequestDurationRecorder;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class GetCacheHandler extends CacheHandler {

    private final ReactiveRepository<PayloadWrapper, String> repository;
    private final CacheConfig config;
    private final ApiConfig apiConfig;
    private final CircuitBreaker circuitBreaker;
    private final Map<String, WebClient> clientsCache;

    @Autowired
    public GetCacheHandler(final ReactiveRepository<PayloadWrapper, String> repository,
                           final CacheConfig config,
                           final ApiConfig apiConfig,
                           final MetricsRecorder metricsRecorder,
                           final PrebidServerResponseBuilder builder,
                           final CircuitBreaker webClientCircuitBreaker,
                           @Value("${sampling.rate:0.01}") final Double samplingRate) {

        super(samplingRate);
        this.metricsRecorder = metricsRecorder;
        this.type = RequestType.FETCH;
        this.repository = repository;
        this.config = config;
        this.apiConfig = apiConfig;
        this.builder = builder;
        this.circuitBreaker = webClientCircuitBreaker;
        this.clientsCache = createClientsCache(config.getClientsCacheDuration(), config.getClientsCacheSize());
    }

    private static Map<String, WebClient> createClientsCache(final int ttl, final int size) {
        return Caffeine.newBuilder()
            .expireAfterWrite(ttl, TimeUnit.SECONDS)
            .maximumSize(size)
            .<String, WebClient>build()
            .asMap();
    }

    public Mono<ServerResponse> fetch(ServerRequest request) {
        // metrics
        metricsRecorder.incrementRequestCount(type);
        final var timerContext = metricsRecorder.createRequestTimer(type);

        return request.queryParam(ID_KEY)
            .map(id -> fetch(request, id, timerContext))
            .orElseGet(() -> finalizeResult(ErrorHandler.createInvalidParameters(), request, timerContext));
    }

    private Mono<ServerResponse> fetch(final ServerRequest request,
                                       final String id,
                                       final RequestDurationRecorder timerContext) {

        final var cacheUrl = resolveCacheUrl(request);

        final var responseMono =
            StringUtils.containsIgnoreCase(cacheUrl, config.getAllowedProxyHost())
                ? processProxyRequest(request, id, cacheUrl)
                : processRequest(request, id);

        return finalizeResult(responseMono, request, timerContext);
    }

    private String resolveCacheUrl(final ServerRequest request) {
        final var cacheHostParam = request.queryParam(CACHE_HOST_KEY).orElse(null);
        if (StringUtils.isNotBlank(cacheHostParam)) {
            return new URIBuilder()
                .setHost(cacheHostParam)
                .setPath(apiConfig.getPath())
                .setScheme(config.getHostParamProtocol())
                .toString();
        }

        return null;
    }

    private Mono<ServerResponse> processProxyRequest(final ServerRequest request,
                                                     final String idKeyParam,
                                                     final String cacheUrl) {

        final WebClient webClient = clientsCache.computeIfAbsent(cacheUrl, WebClient::create);

        return webClient.get()
            .uri(uriBuilder -> uriBuilder.queryParam(ID_KEY, idKeyParam).build())
            .headers(httpHeaders -> httpHeaders.addAll(request.headers().asHttpHeaders()))
            .exchange()
            .transform(CircuitBreakerOperator.of(circuitBreaker))
            .timeout(Duration.ofMillis(config.getTimeoutMs()))
            .subscribeOn(Schedulers.parallel())
            .handle(this::updateProxyMetrics)
            .flatMap(GetCacheHandler::fromClientResponse)
            .doOnError(error -> {
                metricsRecorder.incrementProxyFailureCount();
                log.info("Failed to send request: '{}', cause: '{}'",
                    ExceptionUtils.getMessage(error), ExceptionUtils.getMessage(error));
            });
    }

    private void updateProxyMetrics(final ClientResponse clientResponse,
                                    final SynchronousSink<ClientResponse> sink) {
        if (HttpStatus.OK.equals(clientResponse.statusCode())) {
            metricsRecorder.incrementProxySuccessCount();
        } else {
            metricsRecorder.incrementProxyFailureCount();
        }

        sink.next(clientResponse);
    }

    private static Mono<ServerResponse> fromClientResponse(final ClientResponse clientResponse) {
        return ServerResponse.status(clientResponse.statusCode())
            .headers(headerConsumer -> clientResponse.headers().asHttpHeaders().forEach(headerConsumer::addAll))
            .body(clientResponse.bodyToMono(String.class), String.class);
    }

    private Mono<ServerResponse> processRequest(final ServerRequest request, final String keyIdParam) {
        final var normalizedId = String.format("%s%s", config.getPrefix(), keyIdParam);
        return repository.findById(normalizedId)
            .subscribeOn(Schedulers.parallel())
            .transform(this::validateErrorResult)
            .flatMap(wrapper -> createServerResponse(wrapper, request))
            .switchIfEmpty(ErrorHandler.createResourceNotFound(normalizedId));
    }

    private Mono<ServerResponse> createServerResponse(final PayloadWrapper wrapper, final ServerRequest request) {
        final PayloadType payloadType = PayloadType.from(wrapper.getPayload().getType());
        metricsRecorder.incrementResponseCount(type, payloadType);

        return switch (payloadType) {
            case JSON -> builder.createResponseMono(request, MediaType.APPLICATION_JSON_UTF8, wrapper);
            case XML -> builder.createResponseMono(request, MediaType.APPLICATION_XML, wrapper);
            case UNKNOWN -> Mono.error(new UnsupportedMediaTypeException("Unsupported Media Type."));
        };
    }
}

