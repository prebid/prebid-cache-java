package org.prebid.cache.handlers.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.handlers.MetricsHandler;
import org.prebid.cache.handlers.ServiceType;
import org.prebid.cache.log.ConditionalLogger;
import org.prebid.cache.metrics.MeasurementTag;
import org.prebid.cache.metrics.MetricsRecorder.MetricsRecorderTimer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class CacheHandler extends MetricsHandler {

    private static final int UNKNOWN_SIZE_VALUE = 1;
    ServiceType type;
    static final String ID_KEY = "uuid";
    static final String CACHE_HOST_KEY = "ch";
    private static final String UUID_DUPLICATION = "UUID duplication.";

    protected String metricTagPrefix;

    private final ConditionalLogger conditionalLogger;
    private final Double samplingRate;

    protected CacheHandler(Double samplingRate) {
        this.samplingRate = samplingRate;
        this.conditionalLogger = new ConditionalLogger(log);
    }

    public <T> Mono<T> validateErrorResult(final Mono<T> mono) {
        return mono.doOnSuccess(v -> log.debug("{}: {}", type, v))
                .onErrorMap(DuplicateKeyException.class, error -> {
                    metricsRecorder.getExistingKeyError().increment();
                    return new BadRequestException(UUID_DUPLICATION);
                })
                .onErrorMap(org.springframework.core.codec.DecodingException.class, error ->
                        new RequestParsingException(error.toString()))
                .onErrorMap(org.springframework.web.server.UnsupportedMediaTypeStatusException.class, error ->
                        new UnsupportedMediaTypeException(error.toString()));
    }

    Mono<ServerResponse> finalizeResult(final Mono<ServerResponse> mono,
                                        final ServerRequest request,
                                        final MetricsRecorderTimer timerContext) {
        // transform to error, if needed and send metrics
        return mono
                .onErrorResume(throwable -> handleErrorMetrics(throwable, request))
                .doOnEach(signal -> {
                    if (timerContext != null)
                        timerContext.stop();
                });
    }

    private Mono<ServerResponse> handleErrorMetrics(final Throwable error, final ServerRequest request) {
        if (error instanceof RepositoryException) {
            recordMetric(MeasurementTag.ERROR_DB);
        } else if (error instanceof ResourceNotFoundException || error instanceof BadRequestException) {
            conditionalLogger.info(
                    error.getMessage()
                            + ". Refererring URLs: " + request.headers().header(HttpHeaders.REFERER)
                            + ". Request URI: " + request.uri(),
                    samplingRate);
        } else if (error instanceof TimeoutException) {
            metricsRecorder.markMeterForTag(this.metricTagPrefix, MeasurementTag.ERROR_TIMED_OUT);
        } else if (error instanceof DataBufferLimitException) {
            final long contentLength = request.headers().contentLength().orElse(UNKNOWN_SIZE_VALUE);
            conditionalLogger.error(
                    "Request length: `" + contentLength + "` exceeds maximum size limit",
                    samplingRate);
        } else {
            conditionalLogger.error("Error occurred while processing the request: '%s', cause: '%s'".formatted(
                    ExceptionUtils.getMessage(error), ExceptionUtils.getMessage(error)),
                    samplingRate);
        }

        return builder.error(Mono.just(error), request)
                .doOnNext(response -> handleErrorStatusCodes(request, response));
    }

    private void handleErrorStatusCodes(ServerRequest request, ServerResponse response) {
        final HttpMethod method = request.method();
        if (method == null || response == null) {
            recordMetric(MeasurementTag.ERROR_UNKNOWN);
        } else if (response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            recordMetric(MeasurementTag.ERROR_UNKNOWN);
        } else if (response.statusCode() == HttpStatus.BAD_REQUEST) {
            recordMetric(MeasurementTag.ERROR_BAD_REQUEST);
        } else if (response.statusCode() == HttpStatus.NOT_FOUND) {
            recordMetric(MeasurementTag.ERROR_MISSING_ID);
        }
    }

    private void recordMetric(MeasurementTag tag) {
        metricsRecorder.markMeterForTag(this.metricTagPrefix, tag);
    }

}
