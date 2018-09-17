package org.prebid.cache.handlers;

import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.metrics.GraphiteMetricsRecorder;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.prebid.cache.metrics.MetricsRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static org.springframework.http.MediaType.*;

@Component
@Slf4j
public class GetCacheHandler extends CacheHandler {
    private final ReactiveRepository<PayloadWrapper, String> repository;
    private final CacheConfig config;
    private final static String UNSUPPORTED_MEDIATYPE = "Unsupported Media Type.";

    @Autowired
    public GetCacheHandler(final ReactiveRepository<PayloadWrapper, String> repository,
                           final CacheConfig config,
                           final GraphiteMetricsRecorder metricsRecorder,
                           final PrebidServerResponseBuilder builder) {
        this.metricsRecorder = metricsRecorder;
        this.type = ServiceType.FETCH;
        this.repository = repository;
        this.config = config;
        this.builder = builder;
        this.METRIC_TAG_PREFIX = "read";
    }

    public Mono<ServerResponse> fetch(ServerRequest request) {
        // metrics
        metricsRecorder.markMeterForTag(this.METRIC_TAG_PREFIX, MetricsRecorder.MeasurementTag.REQUEST_RATE);
        val timerContext = metricsRecorder.createRequestContextTimerOptionalForServiceType(this.type)
            .orElse(null);

        return request.queryParam(ID_KEY).map(id -> {
            val normalizedId = String.format("%s%s", config.getPrefix(), id);
            val responseMono = repository.findById(normalizedId)
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .subscribeOn(Schedulers.parallel())
                .transform(this::validateErrorResult)
                .flatMap(wrapper -> {
                    if (wrapper.getPayload().getType().equals(PayloadType.JSON.toString())) {
                        metricsRecorder.markMeterForTag(this.METRIC_TAG_PREFIX, MetricsRecorder.MeasurementTag.JSON_RATE);
                        return builder.createResponseMono(request, APPLICATION_JSON_UTF8, wrapper);
                    } else if (wrapper.getPayload().getType().equals(PayloadType.XML.toString())) {
                        metricsRecorder.markMeterForTag(this.METRIC_TAG_PREFIX, MetricsRecorder.MeasurementTag.XML_RATE);
                        return builder.createResponseMono(request, APPLICATION_XML, wrapper);
                    } else {
                        // unhandled media type
                        return Mono.error(new UnsupportedMediaTypeException(UNSUPPORTED_MEDIATYPE));
                    }
                })
                .switchIfEmpty(ErrorHandler.createResourceNotFound());
            return finalizeResult(responseMono, request, timerContext);
        }).orElseGet(() -> {
            val responseMono = ErrorHandler.createInvalidParameters();
            return finalizeResult(responseMono, request, timerContext);
        });

    }
}

