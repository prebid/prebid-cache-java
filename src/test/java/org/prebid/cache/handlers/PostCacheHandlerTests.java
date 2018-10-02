package org.prebid.cache.handlers;

import com.google.common.collect.ImmutableList;
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

import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

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

    @Test
    void testVerifySave() {
        PostCacheHandler handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder, currentDateProvider);

        val payload = new PayloadTransfer("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "", 1800L, "prebid_");
        val request = Mono.just(new RequestObject(ImmutableList.of(payload)));
        val requestMono = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(request);

        val responseMono = handler.save(requestMono);

        Consumer<ServerResponse> consumer1 = serverResponse -> {
            assertEquals(200, serverResponse.statusCode().value());
        };

        StepVerifier.create(responseMono)
                .consumeNextWith(consumer1)
                .expectComplete()
                .verify();
    }

    @Test
    void testExternalUUIDInvalid() {
        //given
        CacheConfig cacheConfigLocal = new CacheConfig(cacheConfig.getPrefix(), cacheConfig.getExpirySec(), cacheConfig.getTimeoutMs(),
                cacheConfig.getMinExpiry(), cacheConfig.getMaxExpiry(), false);
        PostCacheHandler handler = new PostCacheHandler(repository, cacheConfigLocal, metricsRecorder, builder, currentDateProvider);

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
}
