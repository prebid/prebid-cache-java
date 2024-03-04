package org.prebid.cache.handlers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.config.CircuitBreakerPropertyConfiguration;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.MetricsRecorderTest;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.model.RequestObject;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToIgnoreCase;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.prebid.cache.util.AwaitilityUtil.awaitAndVerify;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    PostCacheHandler.class,
    PrebidServerResponseBuilder.class,
    CacheConfig.class,
    MetricsRecorderTest.class,
    MetricsRecorder.class,
    ApiConfig.class,
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
    CircuitBreaker webClientCircuitBreaker;

    @MockBean
    Supplier<Date> currentDateProvider;

    @MockBean
    ReactiveRepository<PayloadWrapper, String> repository;

    @Value("${sampling.rate:2.0}")
    Double samplingRate;

    @Test
    void testVerifyError() {
        PostCacheHandler handler = new PostCacheHandler(
                repository,
                cacheConfig,
                metricsRecorder,
                builder,
                webClientCircuitBreaker,
                samplingRate);
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
        given(currentDateProvider.get()).willReturn(new Date(100));
        given(repository.save(PAYLOAD_WRAPPER)).willReturn(Mono.just(PAYLOAD_WRAPPER));

        final PostCacheHandler handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder,
                webClientCircuitBreaker, samplingRate);

        final Mono<RequestObject> request = Mono.just(RequestObject.of(Collections.singletonList(PAYLOAD_TRANSFER)));
        final MockServerRequest requestMono = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(request);

        final Mono<ServerResponse> responseMono = handler.save(requestMono);

        final Consumer<ServerResponse> consumer =
                serverResponse -> assertEquals(200, serverResponse.statusCode().value());

        StepVerifier.create(responseMono)
                .consumeNextWith(consumer)
                .expectComplete()
                .verify();
    }

    @Test
    void testSecondaryCacheSuccess() {
        given(currentDateProvider.get()).willReturn(new Date(100));
        given(repository.save(PAYLOAD_WRAPPER)).willReturn(Mono.just(PAYLOAD_WRAPPER));

        serverMock.stubFor(post(urlPathEqualTo("/cache"))
                .willReturn(aResponse().withBody("{\"responses\":[{\"uuid\":\"2be04ba5-8f9b-4a1e-8100-d573c40312f8\"}]}")));

        final PostCacheHandler handler = new PostCacheHandler(repository, cacheConfig, metricsRecorder, builder,
                webClientCircuitBreaker, samplingRate);

        final Mono<RequestObject> request = Mono.just(RequestObject.of(Collections.singletonList(PAYLOAD_TRANSFER)));
        final MockServerRequest requestMono = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(request);

        final Mono<ServerResponse> responseMono = handler.save(requestMono);

        final Consumer<ServerResponse> consumer =
                serverResponse -> assertEquals(200, serverResponse.statusCode().value());

        StepVerifier.create(responseMono)
                .consumeNextWith(consumer)
                .expectComplete()
                .verify();

        final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathEqualTo("/cache"))
                .withQueryParam("secondaryCache", equalTo("yes"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase("application/json"));

        awaitAndVerify(requestPatternBuilder, 5000);
    }

    @Test
    void testExternalUUIDInvalid() {
        //given
        final var cacheConfigLocal = new CacheConfig(cacheConfig.getPrefix(), cacheConfig.getExpirySec(),
            cacheConfig.getTimeoutMs(),
            cacheConfig.getMinExpiry(), cacheConfig.getMaxExpiry(),
            false, Collections.emptyList(), cacheConfig.getSecondaryCachePath(), 100, 100, "example.com", "http");
        final var handler = new PostCacheHandler(repository, cacheConfigLocal, metricsRecorder, builder,
            webClientCircuitBreaker, samplingRate);

        final Mono<RequestObject> request = Mono.just(RequestObject.of(Collections.singletonList(PAYLOAD_TRANSFER)));
        final MockServerRequest requestMono = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(request);

        final Mono<ServerResponse> responseMono = handler.save(requestMono);

        final Consumer<ServerResponse> consumer =
                serverResponse -> assertEquals(400, serverResponse.statusCode().value());

        StepVerifier.create(responseMono)
                .consumeNextWith(consumer)
                .expectComplete()
                .verify();
    }

    @Test
    void testUUIDDuplication() {
        given(currentDateProvider.get()).willReturn(new Date(100));
        given(repository.save(PAYLOAD_WRAPPER))
                .willReturn(Mono.just(PAYLOAD_WRAPPER))
                .willReturn(Mono.error(new DuplicateKeyException("")));

        final CacheConfig cacheConfigLocal = new CacheConfig(cacheConfig.getPrefix(), cacheConfig.getExpirySec(),
                cacheConfig.getTimeoutMs(),
                5, cacheConfig.getMaxExpiry(), cacheConfig.isAllowExternalUUID(),
                Collections.emptyList(), cacheConfig.getSecondaryCachePath(), 100, 100, "example.com", "http");
        final PostCacheHandler handler = new PostCacheHandler(repository, cacheConfigLocal, metricsRecorder, builder,
                webClientCircuitBreaker, samplingRate);

        final Mono<RequestObject> request = Mono.just(RequestObject.of(Collections.singletonList(PAYLOAD_TRANSFER)));
        final MockServerRequest requestMono = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(request);

        final Mono<ServerResponse> responseMono = handler.save(requestMono);

        final Consumer<ServerResponse> consumer =
                serverResponse -> assertEquals(200, serverResponse.statusCode().value());

        StepVerifier.create(responseMono)
                .consumeNextWith(consumer)
                .expectComplete()
                .verify();

        final Mono<ServerResponse> responseMonoSecond = handler.save(requestMono);

        final Consumer<ServerResponse> consumerSecond = serverResponse ->
            assertEquals(400, serverResponse.statusCode().value());

        StepVerifier.create(responseMonoSecond)
            .consumeNextWith(consumerSecond)
            .expectComplete()
            .verify();
    }
}
