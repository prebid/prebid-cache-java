package org.prebid.cache.handlers.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.model.ModulePayload;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.redis.module.storage.ModuleCompositeRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostModuleStorageHandler {

    private final static String API_KEY_HEADER = "x-pbc-api-key";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ModuleCompositeRepository moduleRepository;
    private final PrebidServerResponseBuilder responseBuilder;
    private final ApiConfig apiConfig;

    public Mono<ServerResponse> save(final ServerRequest request) {
        if (!isApiKeyValid(request)) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }

        return getRequestBodyMono(request)
                .flatMap(modulePayload -> moduleRepository.save(
                        modulePayload.getApplication(),
                        mapToPayloadWrapper(modulePayload)))
                .subscribeOn(Schedulers.parallel())
                .onErrorMap(DecodingException.class, error -> new RequestParsingException(error.toString()))
                .onErrorMap(JsonProcessingException.class, error -> new RequestParsingException(error.toString()))
                .onErrorMap(UnsupportedMediaTypeStatusException.class,
                        error -> new UnsupportedMediaTypeException(error.toString()))
                .flatMap(ignored -> ServerResponse.noContent().build())
                .onErrorResume(error -> responseBuilder.error(Mono.just(error), request));
    }

    private Mono<ModulePayload> getRequestBodyMono(final ServerRequest request) {
        return request.body(BodyExtractors.toMono(String.class))
                .handle((value, sink) -> {
                    try {
                        sink.next(objectMapper.readValue(value, ModulePayload.class));
                    } catch (JsonProcessingException e) {
                        sink.error(e);
                    }
                });
    }

    private boolean isApiKeyValid(final ServerRequest request) {
        return StringUtils.equals(request.headers().firstHeader(API_KEY_HEADER), apiConfig.getApiKey());
    }

    private static PayloadWrapper mapToPayloadWrapper(final ModulePayload payload) {
        return PayloadWrapper.builder()
                .id(payload.getKey())
                .prefix(StringUtils.EMPTY)
                .expiry(payload.getTtlseconds().longValue())
                .payload(Payload.of(payload.getType().toString(), payload.getKey(), payload.getValue()))
                .build();
    }
}
