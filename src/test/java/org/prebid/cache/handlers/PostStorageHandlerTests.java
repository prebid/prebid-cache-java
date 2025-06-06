package org.prebid.cache.handlers;

import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.config.StorageConfig;
import org.prebid.cache.handlers.storage.PostStorageHandler;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.model.StoragePayload;
import org.prebid.cache.repository.redis.module.storage.ModuleCompositeRepository;
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

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PrebidServerResponseBuilder.class,
        ApiConfig.class
})
@EnableConfigurationProperties
@SpringBootTest
class PostStorageHandlerTests {

    @Autowired
    ApiConfig apiConfig;

    @Autowired
    PrebidServerResponseBuilder responseBuilder;

    @MockBean
    StorageConfig storageConfig;

    @MockBean
    ModuleCompositeRepository moduleCompositeRepository;

    @MockBean
    Validator validator;

    PostStorageHandler handler;

    WireMockServer serverMock;

    @BeforeEach
    public void setup() {
        handler = new PostStorageHandler(
                validator,
                moduleCompositeRepository,
                responseBuilder,
                apiConfig,
                storageConfig);
        serverMock = new WireMockServer(8080);
        serverMock.start();
    }

    @AfterEach
    public void teardown() {
        serverMock.stop();
    }

    @Test
    void testVerifySave() {
        given(validator.validate(any())).willReturn(Collections.emptySet());

        final var payload = StoragePayload.builder()
                .key("key")
                .type(PayloadType.TEXT)
                .application("application")
                .value("value")
                .ttlseconds(999)
                .build();

        final var payloadWrapper = PayloadWrapper.builder()
                .id("key")
                .prefix("")
                .payload(Payload.of("text", "key", "value"))
                .expiry(999L)
                .build();

        given(moduleCompositeRepository.save("application", payloadWrapper))
                .willReturn(Mono.just(payloadWrapper));

        final var serverRequest = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header("x-pbc-api-key", apiConfig.getApiKey())
                .body(Mono.just(payload));

        final var responseMono = handler.save(serverRequest);

        StepVerifier.create(responseMono)
                .consumeNextWith(serverResponse -> assertEquals(204, serverResponse.statusCode().value()))
                .expectComplete()
                .verify();
    }

    @Test
    void testVerifyDefaultTtl() {
        given(validator.validate(any())).willReturn(Collections.emptySet());
        given(storageConfig.getDefaultTtlSeconds()).willReturn(999L);

        final var payload = StoragePayload.builder()
                .key("key")
                .type(PayloadType.TEXT)
                .application("application")
                .value("value")
                .build();

        final var payloadWrapper = PayloadWrapper.builder()
                .id("key")
                .prefix("")
                .payload(Payload.of("text", "key", "value"))
                .expiry(999L)
                .build();

        given(moduleCompositeRepository.save("application", payloadWrapper))
                .willReturn(Mono.just(payloadWrapper));

        final var serverRequest = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .header("x-pbc-api-key", apiConfig.getApiKey())
                .body(Mono.just(payload));

        final var responseMono = handler.save(serverRequest);

        StepVerifier.create(responseMono)
                .consumeNextWith(serverResponse -> assertEquals(204, serverResponse.statusCode().value()))
                .expectComplete()
                .verify();
    }

    @Test
    void testVerifyApiKeyAuthorization() {
        given(validator.validate(any())).willReturn(Collections.emptySet());

        final var payload = StoragePayload.builder()
                .key("key")
                .type(PayloadType.TEXT)
                .application("application")
                .value("value")
                .ttlseconds(999)
                .build();

        final var serverRequest = MockServerRequest.builder()
                .method(HttpMethod.POST)
                .body(Mono.just(payload));

        final var responseMono = handler.save(serverRequest);

        StepVerifier.create(responseMono)
                .consumeNextWith(serverResponse -> assertEquals(401, serverResponse.statusCode().value()))
                .expectComplete()
                .verify();
    }
}
