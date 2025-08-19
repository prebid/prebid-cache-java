package org.prebid.cache.handlers.storage;

import org.apache.commons.lang3.StringUtils;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.handlers.PayloadType;
import org.prebid.cache.metrics.MeasurementTag;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.module.storage.ModuleCompositeRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Component
public class GetStorageHandler {

    private static final String API_KEY_HEADER = "x-pbc-api-key";

    private static final String KEY = "k";
    private static final String APPLICATION = "a";
    private static final String MODULE_STORAGE_READ_METRIC_PREFIX = "module_storage.read";

    private final ModuleCompositeRepository moduleRepository;
    private final ApiConfig apiConfig;
    private final StorageMetricsRecorder metricsRecorder;

    @Autowired
    public GetStorageHandler(ModuleCompositeRepository moduleRepository,
                             PrebidServerResponseBuilder responseBuilder,
                             ApiConfig apiConfig,
                             MetricsRecorder metricsRecorder) {

        this.moduleRepository = moduleRepository;
        this.apiConfig = apiConfig;
        this.metricsRecorder = new StorageMetricsRecorder(
                responseBuilder, metricsRecorder, MODULE_STORAGE_READ_METRIC_PREFIX);
    }

    public Mono<ServerResponse> fetch(final ServerRequest request) {
        if (!isApiKeyValid(request)) {
            metricsRecorder.recordMetric(MeasurementTag.ERROR_UNAUTHORIZED);
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }

        final String key = request.queryParam(KEY).orElse(null);
        final String application = request.queryParam(APPLICATION).orElse(null);

        if (key == null || application == null) {
            metricsRecorder.recordMetric(MeasurementTag.ERROR_BAD_REQUEST);
            return Mono.error(new BadRequestException("Invalid parameters: key and application are required"));
        }
        metricsRecorder.recordMetric(MeasurementTag.REQUEST);
        final var timerContext = metricsRecorder.createRequestTimer();
        return moduleRepository.findById(application, key)
                .flatMap(this::createServerResponse)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Invalid application or key")))
                .onErrorResume(error -> metricsRecorder.handleErrorMetrics(error, request))
                .doOnEach(signal -> {
                    if (timerContext != null)
                        timerContext.stop();
                });
    }

    private boolean isApiKeyValid(final ServerRequest request) {
        return StringUtils.equals(request.headers().firstHeader(API_KEY_HEADER), apiConfig.getApiKey());
    }

    private Mono<ServerResponse> createServerResponse(final PayloadWrapper wrapper) {
        if (wrapper.getPayload().getType().equals(PayloadType.JSON.toString())) {
            metricsRecorder.recordMetric(MeasurementTag.JSON);
            return ServerResponse.ok().body(fromValue(wrapper.getPayload()));
        } else if (wrapper.getPayload().getType().equals(PayloadType.XML.toString())) {
            metricsRecorder.recordMetric(MeasurementTag.XML);
            return ServerResponse.ok().body(fromValue(wrapper.getPayload()));
        } else if (wrapper.getPayload().getType().equals(PayloadType.TEXT.toString())) {
            metricsRecorder.recordMetric(MeasurementTag.TEXT);
            return ServerResponse.ok().body(fromValue(wrapper.getPayload()));
        }

        return Mono.error(new UnsupportedMediaTypeException("Unsupported Media Type."));
    }
}
