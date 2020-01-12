package org.prebid.cache.handlers;

import com.codahale.metrics.Timer;
import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.exceptions.PrebidException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.metrics.MetricsRecorder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@Slf4j
abstract class CacheHandler extends MetricsHandler {
    ServiceType type;
    static final String ID_KEY = "uuid";
    private static final String UUID_DUPLICATION = "UUID duplication.";

    protected String metricTagPrefix;

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
                .onErrorResume(t -> {
                    // skip overwrite, report first prebid error
                    if (t instanceof PrebidException) {
                        // TODO: 19.09.18 move to handleErrorMetrics
                        applyMetrics(t);

                        if (t instanceof DuplicateKeyException) {
                            return Mono.error(new BadRequestException(UUID_DUPLICATION));
                        }
                        return Mono.error(t);
                    } else if (t instanceof org.springframework.core.codec.DecodingException) {
                        return Mono.error(new RequestParsingException(t.toString()));
                    } else if (t instanceof org.springframework.web.server.UnsupportedMediaTypeStatusException) {
                        return Mono.error(new UnsupportedMediaTypeException(t.toString()));
                    } else {
                        return Mono.error(t);
                    }
                });
    }

    // TODO: 19.09.18 refactor this to normal interaction with mono instead of "casting" exceptions to status codes
    private Mono<ServerResponse> handleErrorMetrics(final Throwable error,
                                                    final ServerRequest request) {
        if (error instanceof ResourceNotFoundException) {
            log.debug(error.getMessage());
        } else if (error instanceof BadRequestException) {
            log.error(error.getMessage());
        } else if (error instanceof TimeoutException) {
            metricsRecorder.markMeterForTag(this.metricTagPrefix, MetricsRecorder.MeasurementTag.ERROR_TIMEDOUT);
        } else {
            log.error("Error occurred while processing the request", error);
        }

        return builder.error(Mono.just(error), request)
                .doAfterSuccessOrError((v, t) -> {
                    HttpMethod method = request.method();
                    if (method == null || t != null || v == null) {
                        metricsRecorder.markMeterForTag(this.metricTagPrefix,
                                MetricsRecorder.MeasurementTag.ERROR_UNKNOWN);
                    } else {
                        if (v.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                            metricsRecorder.markMeterForTag(this.metricTagPrefix,
                                    MetricsRecorder.MeasurementTag.ERROR_UNKNOWN);
                        } else if (v.statusCode() == HttpStatus.BAD_REQUEST) {
                            metricsRecorder.markMeterForTag(this.metricTagPrefix,
                                    MetricsRecorder.MeasurementTag.ERROR_BAD_REQUEST);
                        } else if (v.statusCode() == HttpStatus.NOT_FOUND) {
                            metricsRecorder.markMeterForTag(this.metricTagPrefix,
                                    MetricsRecorder.MeasurementTag.ERROR_MISSINGID);
                        }
                    }
                });
    }

    Mono<ServerResponse> finalizeResult(final Mono<ServerResponse> mono,
                                        final ServerRequest request,
                                        final Timer.Context timerContext) {
        // transform to error, if needed and send metrics
        return mono.onErrorResume(throwable -> handleErrorMetrics(throwable, request))
                .doAfterSuccessOrError((v, t) -> {
                    if (timerContext != null)
                        timerContext.stop();
                });
    }

    void applyMetrics(Throwable t) {
        if (t instanceof RepositoryException) {
            metricsRecorder.markMeterForTag(this.metricTagPrefix, MetricsRecorder.MeasurementTag.ERROR_DB);
        }

        if (t instanceof DuplicateKeyException) {
            metricsRecorder.getExistingKeyError().mark();
        }
    }
}
