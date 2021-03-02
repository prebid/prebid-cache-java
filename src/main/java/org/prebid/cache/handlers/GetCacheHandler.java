package org.prebid.cache.handlers;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Component
@Slf4j
public class GetCacheHandler extends CacheHandler {

    private final ReactiveRepository<PayloadWrapper, String> repository;
    private final CacheConfig config;
    private final CircuitBreaker circuitBreaker;
    private static final String UNSUPPORTED_MEDIATYPE = "Unsupported Media Type.";

    @Autowired
    public GetCacheHandler(final ReactiveRepository<PayloadWrapper, String> repository,
                           final CacheConfig config,
                           final MetricsRecorder metricsRecorder,
                           final PrebidServerResponseBuilder builder,
                           final CircuitBreaker circuitBreaker) {
        this.metricsRecorder = metricsRecorder;
        this.type = ServiceType.FETCH;
        this.repository = repository;
        this.config = config;
        this.builder = builder;
        this.metricTagPrefix = "read";
        this.circuitBreaker = circuitBreaker;
    }

    public Mono<ServerResponse> fetch(ServerRequest request) {
        // metrics
        metricsRecorder.markMeterForTag(this.metricTagPrefix, MetricsRecorder.MeasurementTag.REQUEST);
        val timerContext = metricsRecorder.createRequestContextTimerOptionalForServiceType(this.type)
                .orElse(null);

        return request.queryParam(ID_KEY).map(id -> {
            val normalizedId = String.format("%s%s", config.getPrefix(), id);
            val responseMono = repository.findById(normalizedId)
                    .transform(CircuitBreakerOperator.of(circuitBreaker))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .subscribeOn(Schedulers.parallel())
                    .transform(this::validateErrorResult)
                    .flatMap(wrapper -> {
                        if (wrapper.getPayload().getType().equals(PayloadType.JSON.toString())) {
                            metricsRecorder.markMeterForTag(this.metricTagPrefix, MetricsRecorder.MeasurementTag.JSON);
                            return builder.createResponseMono(request, MediaType.APPLICATION_JSON_UTF8, wrapper);
                        } else if (wrapper.getPayload().getType().equals(PayloadType.XML.toString())) {
                            metricsRecorder.markMeterForTag(this.metricTagPrefix, MetricsRecorder.MeasurementTag.XML);
                            return builder.createResponseMono(request, MediaType.APPLICATION_XML, wrapper);
                        } else {
                            // unhandled media type
                            return Mono.error(new UnsupportedMediaTypeException(UNSUPPORTED_MEDIATYPE));
                        }
                    })
                    .switchIfEmpty(ErrorHandler.createResourceNotFound(normalizedId));
            return finalizeResult(responseMono, request, timerContext);
        }).orElseGet(() -> {
            val responseMono = ErrorHandler.createInvalidParameters();
            return finalizeResult(responseMono, request, timerContext);
        });

    }
}

