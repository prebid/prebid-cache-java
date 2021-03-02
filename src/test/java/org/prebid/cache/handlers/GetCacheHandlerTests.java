package org.prebid.cache.handlers;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.config.CircuitBreakerPropertyConfiguration;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.GraphiteTestConfig;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.routers.ApiConfig;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

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
    GetCacheHandler handler;

    @Autowired
    CircuitBreaker circuitBreaker;

    @MockBean
    ReactiveRepository<PayloadWrapper, String> repository;

    @Test
    void testVerifyError() {
        verifyJacksonError(handler);
        verifyRepositoryError(handler);
    }

    @Test
    void tesVerifyFetch() {
        val payload = new Payload("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", "");
        val payloadWrapper = new PayloadWrapper("12", "prebid_", payload, 1800L, new Date(), true);
        given(repository.findById("prebid_a8db2208-d085-444c-9721-c1161d7f09ce")).willReturn(Mono.just(payloadWrapper));

        val requestMono = MockServerRequest.builder()
                .method(HttpMethod.GET)
                .queryParam("uuid", "a8db2208-d085-444c-9721-c1161d7f09ce")
                .build();

        val responseMono = handler.fetch(requestMono);
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

    }
}
