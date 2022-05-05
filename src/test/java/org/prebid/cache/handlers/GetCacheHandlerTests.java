package org.prebid.cache.handlers;

import com.codahale.metrics.MetricRegistry;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.config.CircuitBreakerPropertyConfiguration;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.GraphiteTestConfig;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        GetCacheHandler.class,
        PrebidServerResponseBuilder.class,
        CacheConfig.class,
        GraphiteTestConfig.class,
        MetricsRecorder.class,
        MetricRegistry.class,
        ApiConfig.class,
        CircuitBreakerPropertyConfiguration.class
})
@EnableConfigurationProperties
@SpringBootTest
class GetCacheHandlerTests extends CacheHandlerTests {

    @Autowired
    CircuitBreaker circuitBreaker;

    @Autowired
    CacheConfig cacheConfig;

    @Autowired
    ApiConfig apiConfig;

    @Autowired
    GraphiteMetricsRecorder metricsRecorder;

    @Autowired
    PrebidServerResponseBuilder responseBuilder;

    @MockBean
    ReactiveRepository<PayloadWrapper, String> repository;

    GetCacheHandler handler;

    WireMockServer serverMock;

    @BeforeEach
    public void setup() {
        handler =
            new GetCacheHandler(repository, cacheConfig, apiConfig, metricsRecorder, responseBuilder, circuitBreaker);
        serverMock = new WireMockServer(8080);
        serverMock.start();
    }

    @AfterEach
    public void teardown() {
        serverMock.stop();
    }

    @Test
    void testVerifyError() {
        verifyJacksonError(handler);
        verifyRepositoryError(handler);
    }

    @Test
    void testVerifyFetch() {
        final var payload = new Payload("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "");
        final var payloadWrapper = new PayloadWrapper("12", "prebid_", payload, 1800L, new Date(), true);
        given(repository.findById("prebid_a8db2208-d085-444c-9721-c1161d7f09ce")).willReturn(Mono.just(payloadWrapper));

        final var requestMono = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .queryParam("uuid", "a8db2208-d085-444c-9721-c1161d7f09ce")
            .build();

        final var responseMono = handler.fetch(requestMono);
        BiConsumer<ServerResponse, Throwable> consumer = (v, t) -> assertEquals(200, v.statusCode().value());

        responseMono.doAfterSuccessOrError(consumer)
            .subscribe();
        StepVerifier.create(responseMono)
            .expectSubscription()
            .expectNextMatches(t -> true)
            .expectComplete()
            .verify();
    }

    @Test
    void testVerifyFetchWithCacheHostParam() {

        serverMock.stubFor(get(urlPathEqualTo("/cache"))
            .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
                .withBody("{\"uuid\":\"2be04ba5-8f9b-4a1e-8100-d573c40312f8\"}")));

        final var requestMono = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
            .queryParam("uuid", "a8db2208-d085-444c-9721-c1161d7f09ce")
            .queryParam("ch", "localhost:8080")
            .build();

        final var responseMono = handler.fetch(requestMono);
        BiConsumer<ServerResponse, Throwable> consumer = (v, t) -> {
            assertEquals(200, v.statusCode().value());
        };

        responseMono.doAfterSuccessOrError(consumer)
            .subscribe();

        StepVerifier.create(responseMono)
            .expectSubscription()
            .expectNextMatches(t -> true)
            .expectComplete()
            .verify();

        verify(getRequestedFor(urlPathEqualTo("/cache"))
            .withQueryParam("uuid", equalTo("a8db2208-d085-444c-9721-c1161d7f09ce"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON_UTF8_VALUE))
        );
    }

    @Test
    void testVerifyFailForNotFoundResourceWithCacheHostParam() {

        final var requestMono = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .queryParam("uuid", "a8db2208-d085-444c-9721-c1161d7f09ce")
            .queryParam("ch", "example.com")
            .build();

        final var responseMono = handler.fetch(requestMono);

        Consumer<ServerResponse> consumer = serverResponse -> {
            assertEquals(404, serverResponse.statusCode().value());
        };

        StepVerifier.create(responseMono)
            .consumeNextWith(consumer)
            .expectComplete()
            .verify();
    }

    @Test
    void testVerifyFetchReturnsBadRequestWhenResponseStatusIsNotOk() {

        serverMock.stubFor(get(urlPathEqualTo("/cache"))
            .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
                .withStatus(201)
                .withBody("{\"uuid\":\"2be04ba5-8f9b-4a1e-8100-d573c40312f8\"}")));

        final var requestMono = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
            .queryParam("uuid", "a8db2208-d085-444c-9721-c1161d7f09ce")
            .queryParam("ch", "localhost:8080")
            .build();

        final var responseMono = handler.fetch(requestMono);
        BiConsumer<ServerResponse, Throwable> consumer = (v, t) -> {
            assertEquals(400, v.statusCode().value());
        };

        responseMono.doAfterSuccessOrError(consumer)
            .subscribe();

        StepVerifier.create(responseMono)
            .expectSubscription()
            .expectNextMatches(t -> true)
            .expectComplete()
            .verify();

        verify(getRequestedFor(urlPathEqualTo("/cache"))
            .withQueryParam("uuid", equalTo("a8db2208-d085-444c-9721-c1161d7f09ce"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON_UTF8_VALUE))
        );
    }
}
