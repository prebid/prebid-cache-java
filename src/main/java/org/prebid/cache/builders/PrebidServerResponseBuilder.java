package org.prebid.cache.builders;


import com.google.common.net.HttpHeaders;
import org.prebid.cache.model.ErrorResponse;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.model.ResponseObject;
import org.prebid.cache.routers.ApiConfig;
import org.prebid.cache.translators.ThrowableTranslator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.server.ServerResponse.status;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Predicate;

@Component
@Slf4j
public class PrebidServerResponseBuilder
{
    final private static String HEADER_CONNECTION_KEEPALIVE = "keep-alive";
    final private static String HEADER_CONNECTION_CLOSE = "close";
    final private ApiConfig apiConfig;

    @Autowired
    public PrebidServerResponseBuilder(final ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    public Mono<ServerResponse> createResponseMono(final ServerRequest request,
                                                   final MediaType mediaType,
                                                   final PayloadWrapper wrapper)
    {
        return ok(request, mediaType).body(fromObject(wrapper.getPayload().getValue()));
    }

    public Mono<ServerResponse> createResponseMono(final ServerRequest request,
                                                   final MediaType mediaType,
                                                   final ResponseObject response)
    {
        return ok(request, mediaType).body(fromObject(response));
    }

    private ServerResponse.BodyBuilder ok(final ServerRequest request, final MediaType mediaType)
    {
        ServerResponse.BodyBuilder builder =
                ServerResponse.ok()
                              .contentType(mediaType)
                              .header(HttpHeaders.DATE, ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                              .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                              .varyBy(HttpHeaders.ACCEPT_ENCODING)
                              .cacheControl(CacheControl.noCache());
        builder = applyHeaders(builder, request);
        return builder;
    }

    public <T extends Throwable> Mono<ServerResponse> error(final Mono<T> monoError,
                                                            final ServerRequest request)
    {
        return monoError.transform(ThrowableTranslator::translate)
                        .flatMap(translation ->
                                    addHeaders(status(translation.getHttpStatus()), request)
                                        .body(Mono.just(new ErrorResponse(
                                                    translation.getHttpStatus().getReasonPhrase(),
                                                    translation.getHttpStatus().value(),
                                                    apiConfig.getPath(),
                                                    translation.getMessage(),
                                                    new Date()
                                                )),
                                            ErrorResponse.class)
                        );
    }

    private static ServerResponse.BodyBuilder addHeaders(final ServerResponse.BodyBuilder builder,
                                                  final ServerRequest request)
    {
        ServerResponse.BodyBuilder headers =
                builder.header(HttpHeaders.DATE, ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                       .varyBy(HttpHeaders.ACCEPT_ENCODING)
                       .cacheControl(CacheControl.noCache());

        headers = applyHeaders(headers, request);
        builder.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        return headers;
    }

    private static ServerResponse.BodyBuilder applyHeaders(final ServerResponse.BodyBuilder builder,
                                                    final ServerRequest request)
    {
        if (isConnectionKeepAlive(request))
            builder.header(HttpHeaders.CONNECTION, HEADER_CONNECTION_KEEPALIVE);
        if (isConnectionClose(request))
            builder.header(HttpHeaders.CONNECTION, HEADER_CONNECTION_CLOSE);
        return builder;
    }

    private static boolean isConnectionKeepAlive(final ServerRequest request) {
        return request.headers()
                      .header(HttpHeaders.CONNECTION)
                      .stream()
                      .map(String::toLowerCase)
                      .allMatch(Predicate.isEqual(PrebidServerResponseBuilder.HEADER_CONNECTION_KEEPALIVE));
    }

    private static boolean isConnectionClose(final ServerRequest request) {
        return request.headers()
                      .header(HttpHeaders.CONNECTION)
                      .stream()
                      .map(String::toLowerCase)
                      .allMatch(Predicate.isEqual(PrebidServerResponseBuilder.HEADER_CONNECTION_CLOSE));
    }
}
