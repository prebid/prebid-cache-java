package org.prebid.cache.handlers.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.model.ModulePayload;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import org.springframework.core.codec.DecodingException;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReactiveRepository<PayloadWrapper, String> repository;

    public Mono<ServerResponse> save(final ServerRequest request) {
        return getRequestBodyMono(request)
                .map(PostModuleStorageHandler::mapToPayloadWrapper)
                .flatMap(repository::save)
                .subscribeOn(Schedulers.parallel())
                .onErrorMap(DecodingException.class, error -> new RequestParsingException(error.toString()))
                .onErrorMap(JsonProcessingException.class, error -> new RequestParsingException(error.toString()))
                .onErrorMap(UnsupportedMediaTypeStatusException.class,
                        error -> new UnsupportedMediaTypeException(error.toString()))
                .flatMap(ignored -> ServerResponse.noContent().build());
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

    private static PayloadWrapper mapToPayloadWrapper(final ModulePayload payload) {
        return PayloadWrapper.builder()
                .id(payload.getKey())
                .prefix(StringUtils.EMPTY)
                .expiry(payload.getTtlseconds().longValue())
                .payload(Payload.of(payload.getType().toString(), payload.getKey(), payload.getValue()))
                .build();
    }
}
