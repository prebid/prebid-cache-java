package org.prebid.cache.handlers.storage;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.repository.redis.module.storage.ModuleCompositeRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Component
@RequiredArgsConstructor
public class GetModuleStorageHandler {

    private static final String API_KEY_HEADER = "x-pbc-api-key";

    private static final String KEY = "key";
    private static final String APPLICATION = "application";

    private final ModuleCompositeRepository moduleRepository;
    private final PrebidServerResponseBuilder responseBuilder;
    private final ApiConfig apiConfig;

    public Mono<ServerResponse> fetch(final ServerRequest request) {
        if (!isApiKeyValid(request)) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }

        final String key = request.queryParam(KEY).orElse(null);
        final String application = request.queryParam(APPLICATION).orElse(null);

        if (key == null || application == null) {
            return Mono.error(new BadRequestException("Invalid parameters: key and application are required"));
        }

        return moduleRepository.findById(application, key)
                .flatMap(value -> ServerResponse.ok().body(fromValue(value.getPayload())))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Invalid application or key")))
                .onErrorResume(error -> responseBuilder.error(Mono.just(error), request));
    }

    private boolean isApiKeyValid(final ServerRequest request) {
        return StringUtils.equals(request.headers().firstHeader(API_KEY_HEADER), apiConfig.getApiKey());
    }
}
