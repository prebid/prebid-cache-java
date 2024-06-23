package org.prebid.cache.handlers.storage;

import lombok.RequiredArgsConstructor;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.handlers.ErrorHandler;
import org.prebid.cache.repository.redis.module.storage.ModuleCompositeRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Component
@RequiredArgsConstructor
public class GetModuleStorageHandler {

    private static final String KEY = "key";
    private static final String APPLICATION = "application";

    private final ModuleCompositeRepository moduleRepository;
    private final PrebidServerResponseBuilder responseBuilder;

    public Mono<ServerResponse> fetch(final ServerRequest request) {
        final String key = request.queryParam(KEY).orElse(null);
        final String application = request.queryParam(APPLICATION).orElse(null);

        if (key == null || application == null) {
            return ErrorHandler.createInvalidParameters();
        }

        return moduleRepository.findById(application, key)
                .flatMap(value -> ServerResponse.ok().body(fromValue(value.getPayload())))
                .switchIfEmpty(ErrorHandler.createResourceNotFound(key))
                .onErrorResume(error -> responseBuilder.error(Mono.just(error), request));
    }
}
