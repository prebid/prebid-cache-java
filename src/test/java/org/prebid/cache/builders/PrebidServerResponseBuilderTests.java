package org.prebid.cache.builders;

import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.routers.ApiConfig;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Date;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.http.MediaType.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={PrebidServerResponseBuilder.class, ApiConfig.class})
@SpringBootTest
class PrebidServerResponseBuilderTests extends PayloadWrapperResponseTests
{
    @Autowired
    PrebidServerResponseBuilder builder;

    private Mono<ServerResponse> createResponseMono(final ServerRequest request,
                                                    final MediaType mediaType)
    {
        if (isJson(mediaType)) {
            return builder.createResponseMono(request, mediaType, jsonPayloadWrapper);
        } else if (isJsonUTF8(mediaType)) {
            return builder.createResponseMono(request, mediaType, jsonUTF8PayloadWrapper);
        }else if (isXml(mediaType)) {
            return builder.createResponseMono(request, mediaType, xmlPayloadWrapper);
        }
        return null;
    }

    private void subscribeAndVerify(final Mono<ServerResponse> mono,
                                    final BiConsumer<ServerResponse, Throwable> consumer)
    {
        mono.doAfterSuccessOrError(consumer)
            .subscribe();
        StepVerifier.create(mono)
                    .expectSubscription()
                    .expectNextMatches(t -> true)
                    .expectComplete()
                    .verify();
    }

    private void verifyServerResponse(MediaType mediaType) {
        final var request = MockServerRequest.builder().build();
        BiConsumer<ServerResponse, Throwable> consumer = (v, t) -> {
            assertEquals(200, v.statusCode().value());
            assertTrue(v.headers().containsValue(Collections.singletonList(mediaType.toString())));
        };

        subscribeAndVerify(createResponseMono(request, mediaType), consumer);
    }

    private void verifyErrorResponse(HttpStatus status) {
        final var request = MockServerRequest.builder().build();
        BiConsumer<ServerResponse, Throwable> consumer =
                (v, t) -> assertEquals(status.value(), v.statusCode().value());

        subscribeAndVerify(createErrorMono(request, status), consumer);
    }

    private Mono<ServerResponse> createErrorMono(final ServerRequest request,
                                                 final HttpStatus status)
    {
        Mono<Exception> mono = Mono.empty();
        if (status.equals(HttpStatus.NOT_FOUND)) {
            mono = Mono.just(new ResourceNotFoundException("not found"));
        } else if (status.equals(HttpStatus.BAD_REQUEST)) {
            mono = Mono.just(new BadRequestException("bad request"));
            return builder.error(mono, request);
        } else if (status.equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
            mono = Mono.just(new RepositoryException("repo error"));
            return builder.error(mono, request);
        }
        return builder.error(mono, request);
    }

    @Test
    void verifyXmlServerResponse() {
        verifyServerResponse(APPLICATION_XML);
    }

    @Test
    void verifyJsonServerResponse() {
        verifyServerResponse(APPLICATION_JSON_UTF8);
    }

    @Test
    void verifyJsonUTF8ServerResponse() { verifyServerResponse(APPLICATION_JSON); }

    @Test
    void verifyNotFound() { verifyErrorResponse(HttpStatus.NOT_FOUND); }

    @Test
    void verifyBadRequest() { verifyErrorResponse(HttpStatus.BAD_REQUEST); }

    @Test
    void verifyRepoError() { verifyErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR); }

    @SpringBootTest
    public abstract static class PayloadTests
    {
        protected static PayloadWrapper jsonPayloadWrapper;
        protected static PayloadWrapper jsonUTF8PayloadWrapper;
        protected static PayloadWrapper xmlPayloadWrapper;

        @BeforeAll
        static void init() {
            jsonPayloadWrapper = createJsonPayloadWrapper();
            jsonUTF8PayloadWrapper = createJsonUTF8PayloadWrapper();
            xmlPayloadWrapper = createXmlPayloadWrapper();
        }

        protected static boolean isXml(MediaType mediaType) {
            return mediaType.equals(APPLICATION_XML);
        }

        protected static boolean isJson(MediaType mediaType) {
            return mediaType.equals(APPLICATION_JSON);
        }

        protected static boolean isJsonUTF8(MediaType mediaType) {
            return mediaType.equals(APPLICATION_JSON_UTF8);
        }

        private static String xmlResponse() {
            return "<?xml version=\"1.0\"?>\n" +
                    "<xml>\n" +
                    "    <creativeCode>\n" +
                    "        <![CDATA[      <html>\n" +
                    "    </html>      ]]>\n" +
                    "</creativeCode>\n" +
                    "</xml>\n";
        }

        private static String jsonResponse() {
            return "{\n" +
                    "  \"creativeCode\" : \"<html></html>\"\n" +
                    "}";
        }

        private static PayloadWrapper createJsonPayloadWrapper() {
            return createPayloadWrapper(APPLICATION_JSON);
        }

        private static PayloadWrapper createJsonUTF8PayloadWrapper() {
            return createPayloadWrapper(APPLICATION_JSON_UTF8);
        }

        private static PayloadWrapper createXmlPayloadWrapper() {
            return createPayloadWrapper(APPLICATION_XML);
        }

        private static PayloadWrapper createPayloadWrapper(MediaType mediaType) {
            String payloadValue = null;
            if (isJson(mediaType) || isJsonUTF8(mediaType)) {
                payloadValue = jsonResponse();
            } else if (isXml(mediaType)) {
                payloadValue = xmlResponse();
            }

            final var payload = new Payload("json", "1234567890", payloadValue);
            return new PayloadWrapper("", "prefix", payload, 200L, new Date(), false);
        }
    }
}
