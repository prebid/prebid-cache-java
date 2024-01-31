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
import org.prebid.cache.metrics.ErrorType;
import org.prebid.cache.metrics.MetricsRecorder.RequestDurationRecorder;
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
    RequestType type;
    static final String ID_KEY = "uuid";
    static final String CACHE_HOST_KEY = "ch";
    private static final String UUID_DUPLICATION = "UUID duplication.";

    private final ConditionalLogger conditionalLogger;
    private final Double samplingRate;

    protected CacheHandler(Double samplingRate) {
        this.samplingRate = samplingRate;
        this.conditionalLogger = new ConditionalLogger(log);
    }

    <T> Mono<T> validateErrorResult(final Mono<T> mono) {
        return mono.doOnSuccess(v -> log.debug("{}: {}", type, v))
            .onErrorMap(DuplicateKeyException.class, error -> {
                metricsRecorder.incrementExistingKeyErrorCount();
                return new BadRequestException(UUID_DUPLICATION);
            })
            .onErrorMap(org.springframework.core.codec.DecodingException.class, error ->
                new RequestParsingException(error.toString()))
            .onErrorMap(org.springframework.web.server.UnsupportedMediaTypeStatusException.class, error ->
                new UnsupportedMediaTypeException(error.toString()));
    }

    Mono<ServerResponse> finalizeResult(final Mono<ServerResponse> mono,
                                        final ServerRequest request,
                                        final RequestDurationRecorder durationRecorder) {

        return mono
            .onErrorResume(throwable -> handleErrorMetrics(throwable, request))
            .doOnEach(signal -> durationRecorder.stop());
    }

    private Mono<ServerResponse> handleErrorMetrics(final Throwable error, final ServerRequest request) {
        if (error instanceof RepositoryException) {
            metricsRecorder.incrementErrorCount(type, ErrorType.DATABASE_ERROR);
        } else if (error instanceof ResourceNotFoundException) {
            conditionalLogger.info(
                error.getMessage()
                    + ". Refererring URLs: " + request.headers().header(HttpHeaders.REFERER)
                    + ". Request URI: " + request.uri(),
                samplingRate);
        } else if (error instanceof BadRequestException) {
            log.error(error.getMessage());
        } else if (error instanceof TimeoutException) {
            metricsRecorder.incrementErrorCount(type, ErrorType.TIMED_OUT);
        } else if (error instanceof DataBufferLimitException) {
            final long contentLength = request.headers().contentLength()
                .orElse(UNKNOWN_SIZE_VALUE);

            conditionalLogger.error(
                "Request length: `" + contentLength + "` exceeds maximum size limit",
                samplingRate);
        } else {
            log.error("Error occurred while processing the request: '{}', cause: '{}'",
                ExceptionUtils.getMessage(error), ExceptionUtils.getMessage(error));
        }

        return builder.error(Mono.just(error), request)
            .doOnEach(signal -> incrementMetrics(request, signal));
    }

    private void incrementMetrics(ServerRequest request, Signal<ServerResponse> signal) {
        final ServerResponse response = signal.get();
        final HttpMethod method = request.method();
        if (method == null || signal.isOnError() || response == null) {
            metricsRecorder.incrementErrorCount(type, ErrorType.UNKNOWN);
        } else {
            if (response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                metricsRecorder.incrementErrorCount(type, ErrorType.UNKNOWN);
            } else if (response.statusCode() == HttpStatus.BAD_REQUEST) {
                metricsRecorder.incrementErrorCount(type, ErrorType.BAD_REQUEST);
            } else if (response.statusCode() == HttpStatus.NOT_FOUND) {
                metricsRecorder.incrementErrorCount(type, ErrorType.MISSING_ID);
            }
        }
    }
}
