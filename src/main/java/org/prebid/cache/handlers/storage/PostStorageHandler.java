package org.prebid.cache.handlers.storage;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.config.StorageConfig;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.model.StoragePayload;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.redis.module.storage.ModuleCompositeRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostStorageHandler {

    private static final String API_KEY_HEADER = "x-pbc-api-key";

    private final Validator validator;
    private final ModuleCompositeRepository moduleRepository;
    private final PrebidServerResponseBuilder responseBuilder;
    private final ApiConfig apiConfig;
    private final StorageConfig storageConfig;

    public Mono<ServerResponse> save(final ServerRequest request) {
        if (!isApiKeyValid(request)) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }

        return request.body(BodyExtractors.toMono(StoragePayload.class))
                .switchIfEmpty(Mono.error(new BadRequestException("Empty body")))
                .handle(this::validateModulePayload)
                .flatMap(storagePayload -> moduleRepository.save(
                        storagePayload.getApplication(),
                        mapToPayloadWrapper(storagePayload)))
                .subscribeOn(Schedulers.parallel())
                .flatMap(ignored -> ServerResponse.noContent().build())
                .onErrorResume(error -> responseBuilder.error(Mono.just(error), request));
    }

    private boolean isApiKeyValid(final ServerRequest request) {
        return StringUtils.equals(request.headers().firstHeader(API_KEY_HEADER), apiConfig.getApiKey());
    }

    private void validateModulePayload(final StoragePayload payload, final SynchronousSink<StoragePayload> sink) {
        final var result = validator.validate(payload);
        if (result.isEmpty()) {
            sink.next(payload);
        } else {
            sink.error(new BadRequestException(
                    result.stream()
                            .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                            .collect(Collectors.joining(", "))));
        }
    }

    private PayloadWrapper mapToPayloadWrapper(final StoragePayload payload) {
        final long ttlSeconds = Optional.ofNullable(payload.getTtlseconds())
                .map(Integer::longValue)
                .orElse(storageConfig.getDefaultTtlSeconds());

        return PayloadWrapper.builder()
                .id(payload.getKey())
                .prefix(StringUtils.EMPTY)
                .expiry(ttlSeconds)
                .payload(Payload.of(payload.getType().toString(), payload.getKey(), payload.getValue()))
                .build();
    }
}
