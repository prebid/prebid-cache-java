package org.prebid.cache.handlers;

import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Disabled;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.helpers.CurrentDateProvider;
import org.prebid.cache.metrics.GraphiteMetricsRecorder;
import org.prebid.cache.metrics.GraphiteTestConfig;
import org.prebid.cache.model.PayloadTransfer;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.model.RequestObject;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.repository.ReactiveTestAerospikeRepositoryContext;
import org.prebid.cache.routers.ApiConfig;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PostCacheHandler.class,
        PrebidServerResponseBuilder.class,
        ReactiveTestAerospikeRepositoryContext.class,
        CacheConfig.class,
        GraphiteTestConfig.class,
        GraphiteMetricsRecorder.class,
        ApiConfig.class,
        CurrentDateProvider.class
})
@EnableConfigurationProperties
@SpringBootTest
@ExtendWith(WireMockExtension.class)
@Disabled
class PostCacheHandlerTests extends CacheHandlerTests {

    @Autowired
    GraphiteMetricsRecorder metricsRecorder;

    @Autowired
    ReactiveRepository<PayloadWrapper, String> repository;

    @Autowired
    PrebidServerResponseBuilder builder;

    @Autowired
    CacheConfig cacheConfig;

    @Autowired
    Supplier<Date> currentDateProvider;

    @Test
    void testVerifyError() {
        PostCacheHandler handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder, currentDateProvider);

        verifyJacksonError(handler);
        verifyRepositoryError(handler);
    }

    @InjectServer
    WireMockServer serverMock;

    @Test
    void testVerifySave() {
        val handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder, currentDateProvider);

        val payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, "prebid_");
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
        serverMock.stubFor(post(urlPathEqualTo("/cache"))
                .willReturn(aResponse().withBody("{\"responses\":[{\"uuid\":\"f31f96db-8c36-4d44-94dc-ad2d1a1d84d9\"}]}")));

        val handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder, currentDateProvider);

        val payload = new PayloadTransfer("json", "f31f96db-8c36-4d44-94dc-ad2d1a1d84d9", "", 1800L, "prebid_");
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

        //do not touch this
        Thread.sleep(10);

        verify(postRequestedFor(urlEqualTo("/cache?secondaryCache=yes")));
    }

    @Test
    void testExternalUUIDInvalid() {
        //given
        val cacheConfigLocal = new CacheConfig(cacheConfig.getPrefix(), cacheConfig.getExpirySec(), cacheConfig.getTimeoutMs(),
                cacheConfig.getMinExpiry(), cacheConfig.getMaxExpiry(), false, Collections.emptyList(), cacheConfig.getSecondaryCachePath());
        val handler = new PostCacheHandler(repository, cacheConfigLocal, metricsRecorder, builder, currentDateProvider);

        val payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, "prebid_");
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
        val cacheConfigLocal = new CacheConfig(cacheConfig.getPrefix(), cacheConfig.getExpirySec(), cacheConfig.getTimeoutMs(),
                5, cacheConfig.getMaxExpiry(), cacheConfig.isAllowExternalUUID(), Collections.emptyList(), cacheConfig.getSecondaryCachePath());
        val handler = new PostCacheHandler(repository, cacheConfigLocal, metricsRecorder, builder, currentDateProvider);

        val payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-a573a84312f1", "", 5L, "prebid_");
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
