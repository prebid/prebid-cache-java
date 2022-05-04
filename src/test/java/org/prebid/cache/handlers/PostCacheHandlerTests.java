package org.prebid.cache.handlers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.ImmutableList;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.config.CircuitBreakerPropertyConfiguration;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.helpers.CurrentDateProvider;
import org.prebid.cache.metrics.GraphiteMetricsRecorder;
import org.prebid.cache.metrics.GraphiteTestConfig;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadTransfer;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.model.RequestObject;
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

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    PostCacheHandler.class,
    PrebidServerResponseBuilder.class,
    CacheConfig.class,
    GraphiteTestConfig.class,
    GraphiteMetricsRecorder.class,
    ApiConfig.class,
    CurrentDateProvider.class,
    CircuitBreakerPropertyConfiguration.class
})
@EnableConfigurationProperties
@SpringBootTest
class PostCacheHandlerTests extends CacheHandlerTests {

    @Autowired
    GraphiteMetricsRecorder metricsRecorder;

    @Autowired
    PrebidServerResponseBuilder builder;

    @Autowired
    CacheConfig cacheConfig;

    @Autowired
    CircuitBreaker circuitBreaker;

    @MockBean
    Supplier<Date> currentDateProvider;

    @MockBean
    ReactiveRepository<PayloadWrapper, String> repository;

    @Test
    void testVerifyError() {
        PostCacheHandler handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder,
            currentDateProvider, circuitBreaker);
        verifyJacksonError(handler);
        verifyRepositoryError(handler);
    }

    WireMockServer serverMock;

    @BeforeEach
    public void setup() {
        serverMock = new WireMockServer(8080);
        serverMock.start();
    }

    @AfterEach
    public void teardown() {
        serverMock.stop();
    }

    @Test
    void testVerifySave() {
        final var payloadInner = new Payload("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "");
        final var payloadWrapper = new PayloadWrapper("2be04ba5-8f9b-4a1e-8100-d573c40312f8", "prebid_", payloadInner
            , 1800L, new Date(100), true);
        given(currentDateProvider.get()).willReturn(new Date(100));
        given(repository.save(payloadWrapper)).willReturn(Mono.just(payloadWrapper));

        final var handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder,
            currentDateProvider, circuitBreaker);

        final var payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, null,
            "prebid_");
        final var request = Mono.just(new RequestObject(ImmutableList.of(payload)));
        final var requestMono = MockServerRequest.builder()
            .method(HttpMethod.POST)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
            .body(request);

        final var responseMono = handler.save(requestMono);

        Consumer<ServerResponse> consumer =
            serverResponse -> assertEquals(200, serverResponse.statusCode().value());

        StepVerifier.create(responseMono)
            .consumeNextWith(consumer)
            .expectComplete()
            .verify();
    }

    @Test
    void testSecondaryCacheSuccess() {
        final var payloadInner = new Payload("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "");
        final var payloadWrapper = new PayloadWrapper("2be04ba5-8f9b-4a1e-8100-d573c40312f8", "prebid_", payloadInner
            , 1800L, new Date(100), true);
        given(currentDateProvider.get()).willReturn(new Date(100));
        given(repository.save(payloadWrapper)).willReturn(Mono.just(payloadWrapper));

        serverMock.stubFor(post(urlPathEqualTo("/cache"))
            .willReturn(aResponse().withBody("{\"responses\":[{\"uuid\":\"2be04ba5-8f9b-4a1e-8100-d573c40312f8\"}]}")));

        final var handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder,
            currentDateProvider, circuitBreaker);

        final var payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, null,
            "prebid_");
        final var request = Mono.just(new RequestObject(ImmutableList.of(payload)));
        final var requestMono = MockServerRequest.builder()
            .method(HttpMethod.POST)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
            .body(request);

        final var responseMono = handler.save(requestMono);

        Consumer<ServerResponse> consumer =
            serverResponse -> assertEquals(200, serverResponse.statusCode().value());

        StepVerifier.create(responseMono)
            .consumeNextWith(consumer)
            .expectComplete()
            .verify();

        await().atLeast(10, TimeUnit.MILLISECONDS);

        verify(postRequestedFor(urlPathEqualTo("/cache"))
            .withQueryParam("secondaryCache", equalTo("yes"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase("application/json")));
    }

    @Test
    void testExternalUUIDInvalid() {
        //given
        final var cacheConfigLocal = new CacheConfig(cacheConfig.getPrefix(), cacheConfig.getExpirySec(),
            cacheConfig.getTimeoutMs(),
            cacheConfig.getMinExpiry(), cacheConfig.getMaxExpiry(),
            false, Collections.emptyList(), cacheConfig.getSecondaryCachePath(), 100, 100, "example.com", "http");
        final var handler = new PostCacheHandler(repository, cacheConfigLocal, metricsRecorder, builder,
            currentDateProvider, circuitBreaker);

        final var payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, null,
            "prebid_");
        final var request = Mono.just(new RequestObject(ImmutableList.of(payload)));
        final var requestMono = MockServerRequest.builder()
            .method(HttpMethod.POST)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
            .body(request);

        final var responseMono = handler.save(requestMono);

        Consumer<ServerResponse> consumer =
            serverResponse -> assertEquals(400, serverResponse.statusCode().value());

        StepVerifier.create(responseMono)
            .consumeNextWith(consumer)
            .expectComplete()
            .verify();
    }

    @Test
    void testUUIDDuplication() {
        final var payloadInner = new Payload("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "");
        final var payloadWrapper = new PayloadWrapper("2be04ba5-8f9b-4a1e-8100-d573c40312f8", "prebid_", payloadInner
            , 1800L, new Date(100), true);
        given(currentDateProvider.get()).willReturn(new Date(100));
        given(repository.save(payloadWrapper)).willReturn(Mono.just(payloadWrapper)).willReturn(Mono.error(new DuplicateKeyException("")));

        final var cacheConfigLocal = new CacheConfig(cacheConfig.getPrefix(), cacheConfig.getExpirySec(),
            cacheConfig.getTimeoutMs(),
            5, cacheConfig.getMaxExpiry(), cacheConfig.isAllowExternalUUID(),
            Collections.emptyList(), cacheConfig.getSecondaryCachePath(), 100, 100, "example.com", "http");
        final var handler = new PostCacheHandler(repository, cacheConfigLocal, metricsRecorder, builder,
            currentDateProvider, circuitBreaker);

        final var payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, null,
            "prebid_");
        final var request = Mono.just(new RequestObject(ImmutableList.of(payload)));
        final var requestMono = MockServerRequest.builder()
            .method(HttpMethod.POST)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
            .body(request);

        final var responseMono = handler.save(requestMono);

        Consumer<ServerResponse> consumer =
            serverResponse -> assertEquals(200, serverResponse.statusCode().value());

        StepVerifier.create(responseMono)
            .consumeNextWith(consumer)
            .expectComplete()
            .verify();

        final var responseMonoSecond = handler.save(requestMono);

        Consumer<ServerResponse> consumerSecond = serverResponse ->
            assertEquals(400, serverResponse.statusCode().value());

        StepVerifier.create(responseMonoSecond)
            .consumeNextWith(consumerSecond)
            .expectComplete()
            .verify();
    }
}
