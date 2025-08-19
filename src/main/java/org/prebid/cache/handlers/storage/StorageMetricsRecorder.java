package org.prebid.cache.handlers.storage;

import lombok.AllArgsConstructor;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.metrics.MeasurementTag;
import org.prebid.cache.metrics.MetricsRecorder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.util.concurrent.TimeoutException;

@AllArgsConstructor
public class StorageMetricsRecorder {

    private final PrebidServerResponseBuilder responseBuilder;
    private final MetricsRecorder metricsRecorder;
    private final String prefix;

    public Mono<ServerResponse> handleErrorMetrics(final Throwable error, final ServerRequest request) {
        if (error instanceof DuplicateKeyException) {
            recordMetric(MeasurementTag.ERROR_DUPLICATE_KEY);
        } else if (error instanceof RepositoryException) {
            recordMetric(MeasurementTag.ERROR_DB);
        } else if (error instanceof ResourceNotFoundException || error instanceof BadRequestException) {
            recordMetric(MeasurementTag.ERROR_BAD_REQUEST);
        } else if (error instanceof TimeoutException) {
            recordMetric(MeasurementTag.ERROR_TIMED_OUT);
        } else {
            recordMetric(MeasurementTag.ERROR_UNKNOWN);
        }

        return responseBuilder.error(Mono.just(error), request)
                .doOnEach(signal -> handleErrorStatusCodes(request, signal));
    }

    private void handleErrorStatusCodes(ServerRequest request, Signal<ServerResponse> signal) {
        final var response = signal.get();
        HttpMethod method = request.method();
        if (method == null || signal.isOnError() || response == null) {
            recordMetric(MeasurementTag.ERROR_UNKNOWN);
        } else if (response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            recordMetric(MeasurementTag.ERROR_UNKNOWN);
        } else if (response.statusCode() == HttpStatus.BAD_REQUEST) {
            recordMetric(MeasurementTag.ERROR_BAD_REQUEST);
        } else if (response.statusCode() == HttpStatus.NOT_FOUND) {
            recordMetric(MeasurementTag.ERROR_MISSING_ID);
        }
    }

    public void recordMetric(MeasurementTag tag) {
        metricsRecorder.markMeterForTag(prefix, tag);
    }

    public MetricsRecorder.MetricsRecorderTimer createRequestTimer() {
        return metricsRecorder.createRequestTimerForTag(prefix);
    }
}
