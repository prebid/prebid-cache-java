package org.prebid.cache.handlers;

import com.aerospike.client.AerospikeException;
import com.codahale.metrics.Timer;
import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.exceptions.PrebidException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.metrics.MetricsRecorder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
abstract class CacheHandler extends MetricsHandler
{
    ServiceType type;
    static String ID_KEY = "uuid";

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

    <T>Mono<T> validateErrorResult(final Mono<T> mono) {
        return mono.doOnSuccess(v -> log.debug("{}: {}", type, v))
                   .onErrorResume(t -> {
                       log.error(t.getMessage(), t);
                        // skip overwrite, report first prebid error
                        if (t instanceof PrebidException) {
                            return Mono.error(t);
                        } else if (t instanceof io.lettuce.core.RedisConnectionException) {
                            return Mono.error(new RepositoryException(t.toString()));
                        } else if (t instanceof org.springframework.core.codec.DecodingException) {
                            return Mono.error(new RequestParsingException(t.toString()));
                        } else if (t instanceof org.springframework.web.server.UnsupportedMediaTypeStatusException) {
                            return Mono.error(new UnsupportedMediaTypeException(t.toString()));
                        } else if (t instanceof AerospikeException) {
                            return Mono.error(new AerospikeException(t.toString()));
                        } else {
                            return Mono.error(t);
                        }
                });
    }

    private Mono<ServerResponse> handleErrorMetrics(final Throwable error,
                                                    final ServerRequest request)
    {
        log.error(error.getMessage(), error);
        return builder.error(Mono.just(error), request)
                      .doAfterSuccessOrError((v, t) -> {
                            HttpMethod method = request.method();
                            if (method == null || t != null || v == null) {
                                metricsRecorder.markMeterForClass(this.getClass(), MetricsRecorder.MeasurementTag.ERROR_RATE);
                            } else {
                                if (v.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                                    metricsRecorder.markMeterForClass(this.getClass(), MetricsRecorder.MeasurementTag.ERROR_RATE);
                                } else if (v.statusCode() == HttpStatus.BAD_REQUEST) {
                                    metricsRecorder.markMeterForClass(this.getClass(), MetricsRecorder.MeasurementTag.BAD_REQUEST_RATE);
                                } else if (v.statusCode() == HttpStatus.NOT_FOUND) {
                                    metricsRecorder.markMeterForClass(this.getClass(), MetricsRecorder.MeasurementTag.RESOURCE_NOT_FOUND_RATE);
                                }
                            }
                      });
    }

    Mono<ServerResponse> finalizeResult(final Mono<ServerResponse> mono,
                                        final ServerRequest request,
                                        final Timer.Context timerContext)
    {
        // transform to error, if needed and send metrics
        return mono.onErrorResume(throwable -> handleErrorMetrics(throwable, request))
                   .doAfterSuccessOrError((v, t) -> {
                       if (timerContext != null)
                           timerContext.stop();
                   });
    }
}
