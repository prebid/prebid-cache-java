package org.prebid.cache.handlers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.log.ConditionalLogger;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.MetricsRecorder.MetricsRecorderTimer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.util.concurrent.TimeoutException;

@Slf4j
abstract class CacheHandler extends MetricsHandler {

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

    protected enum PayloadType implements StringTypeConvertible {
        JSON("json"),
        XML("xml");

        private final String text;

        PayloadType(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    <T> Mono<T> validateErrorResult(final Mono<T> mono) {
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
            recordMetric(MetricsRecorder.MeasurementTag.ERROR_DB);
        } else if (error instanceof ResourceNotFoundException) {
            conditionalLogger.info(
                    error.getMessage()
                            + ". Refererring URLs: " + request.headers().header(HttpHeaders.REFERER)
                            + ". Request URI: " + request.uri(),
                    samplingRate);
        } else if (error instanceof BadRequestException) {
            log.error(error.getMessage());
        } else if (error instanceof TimeoutException) {
            metricsRecorder.markMeterForTag(this.metricTagPrefix, MetricsRecorder.MeasurementTag.ERROR_TIMEDOUT);
        } else if (error instanceof DataBufferLimitException) {
            final long contentLength = request.headers().contentLength().orElse(UNKNOWN_SIZE_VALUE);
            conditionalLogger.error(
                    "Request length: `" + contentLength + "` exceeds maximum size limit",
                    samplingRate);
        } else {
            log.error("Error occurred while processing the request: '{}', cause: '{}'",
                    ExceptionUtils.getMessage(error), ExceptionUtils.getMessage(error));
        }

        return builder.error(Mono.just(error), request)
                .doOnEach(signal -> handleErrorStatusCodes(request, signal));
    }

    private void handleErrorStatusCodes(ServerRequest request, Signal<ServerResponse> signal) {
        final var response = signal.get();
        HttpMethod method = request.method();
        if (method == null || signal.isOnError() || response == null) {
            recordMetric(MetricsRecorder.MeasurementTag.ERROR_UNKNOWN);
        } else if (response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            recordMetric(MetricsRecorder.MeasurementTag.ERROR_UNKNOWN);
        } else if (response.statusCode() == HttpStatus.BAD_REQUEST) {
            recordMetric(MetricsRecorder.MeasurementTag.ERROR_BAD_REQUEST);
        } else if (response.statusCode() == HttpStatus.NOT_FOUND) {
            recordMetric(MetricsRecorder.MeasurementTag.ERROR_MISSINGID);
        }
    }

    private void recordMetric(MetricsRecorder.MeasurementTag tag) {
        metricsRecorder.markMeterForTag(this.metricTagPrefix, tag);
    }

}
