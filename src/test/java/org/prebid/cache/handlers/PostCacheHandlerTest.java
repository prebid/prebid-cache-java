package org.prebid.cache.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.config.CircuitBreakerPropertyConfiguration;
import org.prebid.cache.config.ObjectMapperConfig;
import org.prebid.cache.handlers.cache.CacheHandler;
import org.prebid.cache.handlers.cache.PostCacheHandler;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.MetricsRecorderTest;
import org.prebid.cache.model.PayloadTransfer;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.model.RequestObject;
import org.prebid.cache.model.ResponseObject;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.prebid.cache.util.AwaitilityUtil.awaitAndVerify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CircuitBreakerPropertyConfiguration.class,
        ObjectMapperConfig.class,
        MetricsRecorderTest.class
})
@EnableConfigurationProperties
@SpringBootTest
public class PostCacheHandlerTest {

    @MockBean
    private CacheConfig config;

    @MockBean
    private CacheHandler cacheHandler;

    @MockBean
    private ReactiveRepository<PayloadWrapper, String> repository;

    @Autowired
    private CircuitBreaker webClientCircuitBreaker;

    @MockBean
    private PrebidServerResponseBuilder builder;

    @SpyBean
    private MetricsRecorder metricsRecorder;

    @SpyBean
    private ObjectMapper mapper;

    private PostCacheHandler target;

    private WireMockServer wireMockServer;

    @Mock
    private ServerResponse response;

    @BeforeEach
    public void setup() {
        given(config.getHostParamProtocol()).willReturn("http");
        given(config.getSecondaryUris()).willReturn(singletonList("localhost:8080"));
        given(config.getSecondaryCachePath()).willReturn("/cache");

        given(cacheHandler.validateErrorResult(eq(ServiceType.SAVE), any()))
                .willAnswer(invocation -> invocation.getArgument(1));
        given(cacheHandler.finalizeResult(any(), any(), any(), eq("write")))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(repository.save(any()))
                .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        given(builder.createResponseMono(any(), any(), ArgumentMatchers.<ResponseObject>any()))
                .willReturn(Mono.just(response));

        target = new PostCacheHandler(
                config,
                cacheHandler,
                repository,
                webClientCircuitBreaker,
                builder,
                metricsRecorder,
                mapper);

        wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
    }

    @AfterEach
    public void teardown() {
        wireMockServer.stop();
    }

    @Test
    public void saveShouldUseObjectMapperForPlainText() throws JsonProcessingException {
        // given
        final ServerRequest request = MockServerRequest.builder()
                .header("Content-Type", "text/plain")
                .queryParam("secondaryCache", "yes")
                .body(Mono.just("null"));

        // when
        final Mono<ServerResponse> result = target.save(request);

        // then
        StepVerifier.create(result)
                .expectErrorMessage("No Elements Found.")
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.SAVE), eq("write"));
        verify(mapper).readValue(anyString(), eq(RequestObject.class));
        verify(cacheHandler).validateErrorResult(eq(ServiceType.SAVE), any());
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("write"));
    }

    @Test
    public void saveShouldReturnErrorWhenExternalUuidsDisabled() {
        // given
        final ServerRequest request = MockServerRequest.builder()
                .queryParam("secondaryCache", "yes")
                .body(Mono.just(RequestObject.of(singletonList(PayloadTransfer.builder().key("uuid").build()))));

        // when
        final Mono<ServerResponse> result = target.save(request);

        // then
        StepVerifier.create(result)
                .expectErrorMessage("Prebid cache host forbids specifying UUID in request.")
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.SAVE), eq("write"));
        verify(cacheHandler).validateErrorResult(eq(ServiceType.SAVE), any());
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("write"));
    }

    @Test
    public void saveShouldReturnErrorOnInvalidUuid() {
        // given
        given(config.isAllowExternalUUID()).willReturn(true);

        final ServerRequest request = MockServerRequest.builder()
                .queryParam("secondaryCache", "yes")
                .body(Mono.just(RequestObject.of(singletonList(PayloadTransfer.builder().key("u/uid").build()))));

        // when
        final Mono<ServerResponse> result = target.save(request);

        // then
        StepVerifier.create(result)
                .expectErrorMessage("Invalid UUID: [u/uid].")
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.SAVE), eq("write"));
        verify(cacheHandler).validateErrorResult(eq(ServiceType.SAVE), any());
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("write"));
    }

    @Test
    public void saveShouldReturnExpectedResult() {
        // given
        final ServerRequest request = MockServerRequest.builder()
                .queryParam("secondaryCache", "yes")
                .body(Mono.just(RequestObject.of(singletonList(PayloadTransfer.builder().build()))));

        // when
        final Mono<ServerResponse> result = target.save(request);

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.SAVE), eq("write"));
        verify(repository).save(any());
        verify(cacheHandler).validateErrorResult(eq(ServiceType.SAVE), any());
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("write"));
    }

    @Test
    public void saveShouldSendRequestToSecondaryCacheHosts() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo("/cache"))
                .withQueryParam("secondaryCache", equalTo("yes"))
                .willReturn(aResponse().withStatus(200)));

        final ServerRequest request = MockServerRequest.builder()
                .body(Mono.just(RequestObject.of(singletonList(PayloadTransfer.builder().build()))));

        // when
        final Mono<ServerResponse> result = target.save(request);

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.SAVE), eq("write"));
        verify(repository).save(any());
        verify(cacheHandler).validateErrorResult(eq(ServiceType.SAVE), any());
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("write"));

        awaitAndVerify(
                postRequestedFor(urlPathEqualTo("/cache")).withQueryParam("secondaryCache", equalTo("yes")),
                5000);
    }

    private static <T> Consumer<T> emptyConsumer() {
        return t -> {
        };
    }
}
