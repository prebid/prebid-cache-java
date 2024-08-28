package org.prebid.cache.handlers;

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
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.handlers.cache.CacheHandler;
import org.prebid.cache.handlers.cache.GetCacheHandler;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.MetricsRecorderTest;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CircuitBreakerPropertyConfiguration.class, MetricsRecorderTest.class})
@EnableConfigurationProperties
@SpringBootTest
public class GetCacheHandlerTest {

    @MockBean
    private CacheConfig config;

    @MockBean
    private ApiConfig apiConfig;

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

    private GetCacheHandler target;

    private WireMockServer wireMockServer;

    @Mock
    private ServerResponse response;

    @BeforeEach
    public void setup() {
        given(config.getPrefix()).willReturn("prefix:");
        given(config.getHostParamProtocol()).willReturn("http");
        given(config.getTimeoutMs()).willReturn(Integer.MAX_VALUE);

        given(apiConfig.getCachePath()).willReturn("cache");

        given(cacheHandler.validateErrorResult(eq(ServiceType.FETCH), any()))
                .willAnswer(invocation -> invocation.getArgument(1));
        given(cacheHandler.finalizeResult(any(), any(), any(), eq("read")))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(builder.createResponseMono(any(), any(), ArgumentMatchers.<PayloadWrapper>any()))
                .willReturn(Mono.just(response));

        target = new GetCacheHandler(
                config,
                apiConfig,
                cacheHandler,
                repository,
                webClientCircuitBreaker,
                builder,
                metricsRecorder);

        wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
    }

    @AfterEach
    public void teardown() {
        wireMockServer.stop();
    }

    @Test
    public void fetchShouldReturnErrorOnMissingUuid() {
        // given
        given(cacheHandler.finalizeResult(any(), any(), any(), anyString()))
                .willReturn(Mono.error(new Throwable("error")));

        final ServerRequest request = MockServerRequest.builder().build();

        // when
        final Mono<ServerResponse> result = target.fetch(request);

        // then
        StepVerifier.create(result)
                .expectErrorMessage("error")
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.FETCH), eq("read"));
    }

    @Test
    public void fetchShouldReturnResourceNotFound() {
        // given
        given(repository.findById(eq("prefix:uuid"))).willReturn(Mono.empty());

        final ServerRequest request = MockServerRequest.builder()
                .queryParam("uuid", "uuid")
                .build();

        // when
        final Mono<ServerResponse> result = target.fetch(request);

        // then
        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.FETCH), eq("read"));
        verify(cacheHandler).validateErrorResult(eq(ServiceType.FETCH), any());
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("read"));
    }

    @Test
    public void fetchShouldProcessJsonPayload() {
        // given
        final PayloadWrapper payloadWrapper = PayloadWrapper.builder()
                .payload(Payload.of("json", null, null))
                .build();
        given(repository.findById(eq("prefix:uuid"))).willReturn(Mono.just(payloadWrapper));

        final ServerRequest request = MockServerRequest.builder()
                .queryParam("uuid", "uuid")
                .build();

        // when
        final Mono<ServerResponse> result = target.fetch(request);

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.FETCH), eq("read"));
        verify(cacheHandler).validateErrorResult(eq(ServiceType.FETCH), any());
        verify(metricsRecorder).markMeterForTag(eq("read"), eq(MetricsRecorder.MeasurementTag.JSON));
        verify(builder).createResponseMono(any(), eq(MediaType.APPLICATION_JSON_UTF8), same(payloadWrapper));
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("read"));
    }

    @Test
    public void fetchShouldProcessXmlPayload() {
        // given
        final PayloadWrapper payloadWrapper = PayloadWrapper.builder()
                .payload(Payload.of("xml", null, null))
                .build();
        given(repository.findById(eq("prefix:uuid"))).willReturn(Mono.just(payloadWrapper));

        final ServerRequest request = MockServerRequest.builder()
                .queryParam("uuid", "uuid")
                .build();

        // when
        final Mono<ServerResponse> result = target.fetch(request);

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.FETCH), eq("read"));
        verify(cacheHandler).validateErrorResult(eq(ServiceType.FETCH), any());
        verify(metricsRecorder).markMeterForTag(eq("read"), eq(MetricsRecorder.MeasurementTag.XML));
        verify(builder).createResponseMono(any(), eq(MediaType.APPLICATION_XML), same(payloadWrapper));
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("read"));
    }

    @Test
    public void fetchShouldProcessUnsupportedPayload() {
        // given
        final PayloadWrapper payloadWrapper = PayloadWrapper.builder()
                .payload(Payload.of("unsupported", null, null))
                .build();
        given(repository.findById(eq("prefix:uuid"))).willReturn(Mono.just(payloadWrapper));

        final ServerRequest request = MockServerRequest.builder()
                .queryParam("uuid", "uuid")
                .build();

        // when
        final Mono<ServerResponse> result = target.fetch(request);

        // then
        StepVerifier.create(result)
                .expectErrorMessage("Unsupported Media Type.")
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.FETCH), eq("read"));
        verify(cacheHandler).validateErrorResult(eq(ServiceType.FETCH), any());
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("read"));
    }

    @Test
    public void fetchShouldProcessProxyRequest() {
        // given
        given(config.getAllowedProxyHost()).willReturn("http://localhost:8080/cache");
        wireMockServer.stubFor(get(urlPathEqualTo("/cache"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
                        .withBody("{\"uuid\":\"uuid\"}")));

        final ServerRequest request = MockServerRequest.builder()
                .queryParam("uuid", "uuid")
                .queryParam("ch", "localhost:8080")
                .build();

        // when
        final Mono<ServerResponse> result = target.fetch(request);

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.FETCH), eq("read"));
        verify(metricsRecorder).getProxySuccess();
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("read"));
    }

    @Test
    public void fetchShouldProcessProxyRequestWithWrongStatusCode() {
        // given
        given(config.getAllowedProxyHost()).willReturn("http://localhost:8080/cache");
        wireMockServer.stubFor(get(urlPathEqualTo("/cache"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
                        .withBody("{\"uuid\":\"uuid\"}")));

        final ServerRequest request = MockServerRequest.builder()
                .queryParam("uuid", "uuid")
                .queryParam("ch", "localhost:8080")
                .build();

        // when
        final Mono<ServerResponse> result = target.fetch(request);

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.FETCH), eq("read"));
        verify(metricsRecorder).getProxyFailure();
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("read"));
    }

    @Test
    public void fetchShouldProcessFailedProxyRequest() {
        // given
        given(config.getAllowedProxyHost()).willReturn("http://unreachable/cache");

        final ServerRequest request = MockServerRequest.builder()
                .queryParam("uuid", "uuid")
                .queryParam("ch", "unreachable")
                .build();

        // when
        final Mono<ServerResponse> result = target.fetch(request);

        // then
        StepVerifier.create(result)
                .expectError()
                .verify();
        verify(cacheHandler).timerContext(eq(ServiceType.FETCH), eq("read"));
        verify(metricsRecorder).getProxyFailure();
        verify(cacheHandler).finalizeResult(any(), any(), any(), eq("read"));
    }

    private static <T> Consumer<T> emptyConsumer() {
        return t -> {
        };
    }
}
