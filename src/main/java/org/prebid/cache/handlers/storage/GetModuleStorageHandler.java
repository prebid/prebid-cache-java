package org.prebid.cache.handlers.storage;

import lombok.RequiredArgsConstructor;
import org.prebid.cache.handlers.ErrorHandler;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Component
@RequiredArgsConstructor
public class GetModuleStorageHandler {

    private static final String KEY = "key";

    private final ReactiveRepository<PayloadWrapper, String> repository;

    public Mono<ServerResponse> fetch(final ServerRequest request) {
        return request.queryParam(KEY)
                .map(key -> repository.findById(key)
                        .flatMap(value -> ServerResponse.ok().body(fromValue(value.getPayload())))
                        .switchIfEmpty(ErrorHandler.createResourceNotFound(key)))
                .orElseGet(ErrorHandler::createInvalidParameters);
    }
}
