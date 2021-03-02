package org.prebid.cache.handlers;

import com.codahale.metrics.MetricRegistry;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.ImmutableList;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.config.CircuitBreakerPropertyConfiguration;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.helpers.CurrentDateProvider;
import org.prebid.cache.metrics.MetricsRecorder;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
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
        MetricsRecorder.class,
        MetricRegistry.class,
        ApiConfig.class,
        CurrentDateProvider.class,
        CircuitBreakerPropertyConfiguration.class
})
@EnableConfigurationProperties
@SpringBootTest
class PostCacheHandlerTests extends CacheHandlerTests {

    @Autowired
    MetricsRecorder metricsRecorder;

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
        PostCacheHandler handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder, currentDateProvider, circuitBreaker);
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
        val payloadInner = new Payload("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "");
        val payloadWrapper = new PayloadWrapper("2be04ba5-8f9b-4a1e-8100-d573c40312f8", "prebid_", payloadInner, 1800L, new Date(100), true);
        given(currentDateProvider.get()).willReturn(new Date(100));
        given(repository.save(payloadWrapper)).willReturn(Mono.just(payloadWrapper));

        val handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder, currentDateProvider, circuitBreaker);

        val payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, null, "prebid_");
        val request = Mono.just(new RequestObject(ImmutableList.of(payload)));
        val requestMono = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(request);

        val responseMono = handler.save(requestMono);

        Consumer<ServerResponse> consumer = serverResponse -> {
            assertEquals(200, serverResponse.statusCode().value());
        };

        StepVerifier.create(responseMono)
                .consumeNextWith(consumer)
                .expectComplete()
                .verify();
    }

    @Test
    void testSecondaryCacheSuccess() throws InterruptedException {
        val payloadInner = new Payload("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "");
        val payloadWrapper = new PayloadWrapper("2be04ba5-8f9b-4a1e-8100-d573c40312f8", "prebid_", payloadInner, 1800L, new Date(100), true);
        given(currentDateProvider.get()).willReturn(new Date(100));
        given(repository.save(payloadWrapper)).willReturn(Mono.just(payloadWrapper));

        serverMock.stubFor(post(urlPathEqualTo("/cache"))
                .willReturn(aResponse().withBody("{\"responses\":[{\"uuid\":\"2be04ba5-8f9b-4a1e-8100-d573c40312f8\"}]}")));

        val handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder, currentDateProvider, circuitBreaker);

        val payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, null, "prebid_");
        val request = Mono.just(new RequestObject(ImmutableList.of(payload)));
        val requestMono = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(request);

        val responseMono = handler.save(requestMono);

        Consumer<ServerResponse> consumer = serverResponse -> {
            assertEquals(200, serverResponse.statusCode().value());
        };

        StepVerifier.create(responseMono)
                .consumeNextWith(consumer)
                .expectComplete()
                .verify();

        await().atLeast(10, TimeUnit.MILLISECONDS);

        verify(postRequestedFor(urlEqualTo("/cache?secondaryCache=yes")));
    }

    @Test
    void testExternalUUIDInvalid() {
        //given
        val cacheConfigLocal = new CacheConfig(cacheConfig.getPrefix(), cacheConfig.getExpirySec(), cacheConfig.getTimeoutMs(),
                cacheConfig.getMinExpiry(), cacheConfig.getMaxExpiry(), false, Collections.emptyList(), cacheConfig.getSecondaryCachePath());
        val handler = new PostCacheHandler(repository, cacheConfigLocal, metricsRecorder, builder, currentDateProvider, circuitBreaker);

        val payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, null, "prebid_");
        val request = Mono.just(new RequestObject(ImmutableList.of(payload)));
        val requestMono = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(request);

        val responseMono = handler.save(requestMono);

        Consumer<ServerResponse> consumer = serverResponse -> {
            assertEquals(400, serverResponse.statusCode().value());
        };

        StepVerifier.create(responseMono)
                .consumeNextWith(consumer)
                .expectComplete()
                .verify();
    }

    @Test
    void testUUIDDuplication() {
        val payloadInner = new Payload("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "");
        val payloadWrapper = new PayloadWrapper("2be04ba5-8f9b-4a1e-8100-d573c40312f8", "prebid_", payloadInner, 1800L, new Date(100), true);
        given(currentDateProvider.get()).willReturn(new Date(100));
        given(repository.save(payloadWrapper)).willReturn(Mono.just(payloadWrapper)).willReturn(Mono.error(new DuplicateKeyException("")));

        val cacheConfigLocal = new CacheConfig(cacheConfig.getPrefix(), cacheConfig.getExpirySec(), cacheConfig.getTimeoutMs(),
                5, cacheConfig.getMaxExpiry(), cacheConfig.isAllowExternalUUID(), Collections.emptyList(), cacheConfig.getSecondaryCachePath());
        val handler = new PostCacheHandler(repository, cacheConfigLocal, metricsRecorder, builder, currentDateProvider, circuitBreaker);

        val payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, null, "prebid_");
        val request = Mono.just(new RequestObject(ImmutableList.of(payload)));
        val requestMono = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(request);

        val responseMono = handler.save(requestMono);

        Consumer<ServerResponse> consumer = serverResponse -> {
            assertEquals(200, serverResponse.statusCode().value());
        };

        StepVerifier.create(responseMono)
                .consumeNextWith(consumer)
                .expectComplete()
                .verify();

        val responseMonoSecond = handler.save(requestMono);

        Consumer<ServerResponse> consumerSecond = serverResponse -> {
            assertEquals(400, serverResponse.statusCode().value());
        };

        StepVerifier.create(responseMonoSecond)
                .consumeNextWith(consumerSecond)
                .expectComplete()
                .verify();

    }
}
