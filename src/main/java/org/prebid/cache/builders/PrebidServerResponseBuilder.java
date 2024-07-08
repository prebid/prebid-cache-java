package org.prebid.cache.builders;

import com.google.common.net.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.model.ErrorResponse;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.model.ResponseObject;
import org.prebid.cache.routers.ApiConfig;
import org.prebid.cache.translators.ThrowableTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

@Component
@Slf4j
public class PrebidServerResponseBuilder {

    private static final String HEADER_CONNECTION_KEEPALIVE = "keep-alive";
    private static final String HEADER_CONNECTION_CLOSE = "close";

    private final ApiConfig apiConfig;

    @Autowired
    public PrebidServerResponseBuilder(final ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    public Mono<ServerResponse> createResponseMono(final ServerRequest request,
                                                   final MediaType mediaType,
                                                   final PayloadWrapper wrapper) {
        return ok(request, mediaType).body(fromValue(wrapper.getPayload().getValue()));
    }

    public Mono<ServerResponse> createResponseMono(final ServerRequest request,
                                                   final MediaType mediaType,
                                                   final ResponseObject response) {
        return ok(request, mediaType).body(fromValue(response));
    }

    private ServerResponse.BodyBuilder ok(final ServerRequest request, final MediaType mediaType) {
        final String now = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        ServerResponse.BodyBuilder builder = ServerResponse.ok()
                                                     .contentType(mediaType)
                                                     .header(HttpHeaders.DATE, now)
                                                     .varyBy(HttpHeaders.ACCEPT_ENCODING)
                                                     .cacheControl(CacheControl.noCache());
        applyHeaders(builder, request);
        return builder;
    }

    public <T extends Throwable> Mono<ServerResponse> error(final Mono<T> monoError,
                                                            final ServerRequest request) {
        return monoError.transform(ThrowableTranslator::translate)
                .flatMap(translation ->
                        addHeaders(status(translation.getHttpStatus()), request)
                                .body(Mono.just(
                                        ErrorResponse.builder()
                                                .error(translation.getHttpStatus().getReasonPhrase())
                                                .status(translation.getHttpStatus().value())
                                                .path(request.path())
                                                .message(translation.getErrorMessage())
                                                .timestamp(new Date())
                                                .build()),
                                        ErrorResponse.class)
                );
    }

    private static ServerResponse.BodyBuilder addHeaders(final ServerResponse.BodyBuilder builder,
                                                         final ServerRequest request) {
        ServerResponse.BodyBuilder headers =
                builder.header(HttpHeaders.DATE, ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                        .varyBy(HttpHeaders.ACCEPT_ENCODING)
                        .cacheControl(CacheControl.noCache());

        return applyHeaders(headers, request);
    }

    private static ServerResponse.BodyBuilder applyHeaders(final ServerResponse.BodyBuilder builder,
                                                           final ServerRequest request) {

        final List<String> connectionHeaders = request.headers().header(HttpHeaders.CONNECTION);
        if (hasConnectionValue(connectionHeaders, HEADER_CONNECTION_KEEPALIVE)) {
            builder.header(HttpHeaders.CONNECTION, HEADER_CONNECTION_KEEPALIVE);
        }
        if (hasConnectionValue(connectionHeaders, HEADER_CONNECTION_CLOSE)) {
            builder.header(HttpHeaders.CONNECTION, HEADER_CONNECTION_CLOSE);
        }
        return builder;
    }

    private static boolean hasConnectionValue(List<String> connectionHeaders, String value) {
        return !connectionHeaders.isEmpty() && connectionHeaders.stream()
                                                       .map(String::toLowerCase)
                                                       .allMatch(Predicate.isEqual(value));
    }

}
