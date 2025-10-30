package org.prebid.cache.handlers;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.handlers.storage.GetStorageHandler;
import org.prebid.cache.metrics.MeasurementTag;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.module.storage.ModuleCompositeRepository;
import org.prebid.cache.routers.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        GetStorageHandler.class,
        PrebidServerResponseBuilder.class,
        ApiConfig.class
})
@EnableConfigurationProperties
@SpringBootTest
public class GetStorageHandlerTests {

    @Autowired
    ApiConfig apiConfig;

    @Autowired
    PrebidServerResponseBuilder responseBuilder;

    @MockBean
    MetricsRecorder metricsRecorder;

    @MockBean
    ModuleCompositeRepository moduleCompositeRepository;

    GetStorageHandler handler;

    WireMockServer serverMock;

    @BeforeEach
    public void setup() {
        handler = new GetStorageHandler(moduleCompositeRepository, responseBuilder, apiConfig, metricsRecorder);
        serverMock = new WireMockServer(8080);
        serverMock.start();
    }

    @AfterEach
    public void teardown() {
        serverMock.stop();
        reset(metricsRecorder);
    }

    @Test
    void testVerifyApiKeyAuthorization() {
        final var serverRequest = MockServerRequest.builder()
                .method(HttpMethod.GET)
                .build();

        final var responseMono = handler.fetch(serverRequest);

        StepVerifier.create(responseMono)
                .consumeNextWith(serverResponse -> assertEquals(401, serverResponse.statusCode().value()))
                .expectComplete()
                .verify();

        verify(metricsRecorder).markMeterForTag("module_storage.read", MeasurementTag.ERROR_UNAUTHORIZED);
    }

    @Test
    void testVerifyFetch() {
        final var payloadWrapper = PayloadWrapper.builder()
                .id("key")
                .prefix("")
                .payload(Payload.of("text", "key", "value"))
                .expiry(999L)
                .build();

        given(moduleCompositeRepository.findById("application", "key"))
                .willReturn(Mono.just(payloadWrapper));

        final var serverRequest = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header("x-pbc-api-key", apiConfig.getApiKey())
                .queryParam("k", "key")
                .queryParam("a", "application")
                .build();

        final var responseMono = handler.fetch(serverRequest);

        StepVerifier.create(responseMono)
                .consumeNextWith(serverResponse -> assertEquals(200, serverResponse.statusCode().value()))
                .expectComplete()
                .verify();

        verify(metricsRecorder).markMeterForTag("module_storage.read", MeasurementTag.REQUEST);
    }
}
