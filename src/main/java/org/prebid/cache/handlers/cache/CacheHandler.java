package org.prebid.cache.handlers.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.handlers.ServiceType;
import org.prebid.cache.log.ConditionalLogger;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.MetricsRecorder.MetricsRecorderTimer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class CacheHandler {

    private static final ConditionalLogger CONDITIONAL_LOGGER = new ConditionalLogger(log);

    public static final String ID_KEY = "uuid";
    public static final String CACHE_HOST_KEY = "ch";
    private static final int UNKNOWN_SIZE_VALUE = 1;

    private final PrebidServerResponseBuilder builder;
    private final MetricsRecorder metricsRecorder;
    private final double samplingRate;

    @Autowired
    public CacheHandler(PrebidServerResponseBuilder builder,
                        MetricsRecorder metricsRecorder,
                        @Value("${sampling.rate:0.01}") double samplingRate) {

        this.builder = Objects.requireNonNull(builder);
        this.metricsRecorder = Objects.requireNonNull(metricsRecorder);
        this.samplingRate = samplingRate;
    }

    public MetricsRecorder.MetricsRecorderTimer timerContext(ServiceType type, String metricTagPrefix) {
        metricsRecorder.markMeterForTag(metricTagPrefix, MetricsRecorder.MeasurementTag.REQUEST);
        return metricsRecorder.createRequestTimerForServiceType(type);
    }

    public <T> Mono<T> validateErrorResult(ServiceType type, Mono<T> mono) {
        return mono
                .doOnSuccess(v -> log.debug("{}: {}", type, v))
                .onErrorMap(DuplicateKeyException.class, error -> {
                    metricsRecorder.getExistingKeyError().increment();
                    return new BadRequestException("UUID duplication.");
                })
                .onErrorMap(DecodingException.class, error -> new RequestParsingException(error.toString()))
                .onErrorMap(UnsupportedMediaTypeStatusException.class, error ->
                        new UnsupportedMediaTypeException(error.toString()));
    }

    public Mono<ServerResponse> finalizeResult(Mono<ServerResponse> mono,
                                               ServerRequest request,
                                               MetricsRecorderTimer timerContext,
                                               String metricTagPrefix) {

        // transform to error, if needed and send metrics
        return mono
                .onErrorResume(throwable -> handleErrorMetrics(throwable, request, metricTagPrefix))
                .doOnNext(ignored -> {
                    if (timerContext != null) {
                        timerContext.stop();
                    }
                });
    }

    private Mono<ServerResponse> handleErrorMetrics(Throwable error, ServerRequest request, String metricTagPrefix) {
        switch (error) {
            case RepositoryException repositoryException ->
                    metricsRecorder.markMeterForTag(metricTagPrefix, MetricsRecorder.MeasurementTag.ERROR_DB);
            case ResourceNotFoundException resourceNotFoundException -> CONDITIONAL_LOGGER.info(
                    error.getMessage()
                            + ". Refererring URLs: " + request.headers().header(HttpHeaders.REFERER)
                            + ". Request URI: " + request.uri(),
                    samplingRate);
            case BadRequestException badRequestException -> log.error(error.getMessage());
            case TimeoutException timeoutException ->
                    metricsRecorder.markMeterForTag(metricTagPrefix, MetricsRecorder.MeasurementTag.ERROR_TIMEDOUT);
            case DataBufferLimitException dataBufferLimitException -> {
                final long contentLength = request.headers().contentLength().orElse(UNKNOWN_SIZE_VALUE);
                CONDITIONAL_LOGGER.error(
                        "Request length: `" + contentLength + "` exceeds maximum size limit",
                        samplingRate);
            }
            default -> log.error(
                    "Error occurred while processing the request: '{}', cause: '{}'",
                    ExceptionUtils.getMessage(error),
                    ExceptionUtils.getMessage(error));
        }

        return builder.error(Mono.just(error), request)
                .doOnSuccess(response -> handleErrorStatusCodes(response, metricTagPrefix));
    }

    private void handleErrorStatusCodes(ServerResponse response, String metricTagPrefix) {
        if (response == null || response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            metricsRecorder.markMeterForTag(metricTagPrefix, MetricsRecorder.MeasurementTag.ERROR_UNKNOWN);
        } else if (response.statusCode() == HttpStatus.BAD_REQUEST) {
            metricsRecorder.markMeterForTag(metricTagPrefix, MetricsRecorder.MeasurementTag.ERROR_BAD_REQUEST);
        } else if (response.statusCode() == HttpStatus.NOT_FOUND) {
            metricsRecorder.markMeterForTag(metricTagPrefix, MetricsRecorder.MeasurementTag.ERROR_MISSINGID);
        }
    }
}
