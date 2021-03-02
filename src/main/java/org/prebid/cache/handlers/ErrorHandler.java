package org.prebid.cache.handlers;

import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.metrics.MetricsRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ErrorHandler extends MetricsHandler {
    private static final String RESOURCE_NOT_FOUND_BAD_URL = "Resource Not Found - Bad URL.";
    private static final String RESOURCE_NOT_FOUND = "Resource Not Found: uuid %s";
    private static final String INVALID_PARAMETERS = "Invalid Parameter(s): uuid not found.";
    private static final String NO_ELEMENTS_FOUND = "No Elements Found.";

    @Autowired
    public ErrorHandler(final MetricsRecorder metricsRecorder, final PrebidServerResponseBuilder builder) {
        this.metricsRecorder = metricsRecorder;
        this.builder = builder;
    }

    static Mono<ServerResponse> createResourceNotFound(String uuid) {
        return Mono.error(new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND, uuid)));
    }

    static Mono<ServerResponse> createInvalidParameters() {
        return Mono.error(new BadRequestException(INVALID_PARAMETERS));
    }

    static Mono<ServerResponse> createNoElementsFound() {
        return Mono.error(new BadRequestException(NO_ELEMENTS_FOUND));
    }

    public Mono<ServerResponse> invalidRequest(final ServerRequest request) {
        metricsRecorder.getInvalidRequestMeter().mark();
        return builder.error(Mono.just(new ResourceNotFoundException(RESOURCE_NOT_FOUND_BAD_URL)), request);
    }
}
