package org.prebid.cache.handlers;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import com.google.common.collect.ImmutableList;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.helpers.CurrentDateProvider;
import org.prebid.cache.metrics.GraphiteMetricsRecorder;
import org.prebid.cache.metrics.GraphiteTestConfig;
import org.prebid.cache.model.PayloadTransfer;
import org.prebid.cache.model.RequestObject;
import org.prebid.cache.repository.CacheConfig;
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

import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
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
class PostCacheHandlerTests extends CacheHandlerTests {

    @Autowired
    PostCacheHandler handler;

    @Test
    void testVerifyError() {
        verifyJacksonError(handler);
        verifyRepositoryError(handler);
    }

    @InjectServer
    WireMockServer serverMock;

    @Test
    void testVerifySave() {
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
                .willReturn(aResponse().withBody("{\"responses\":[{\"uuid\":\"2be04ba5-8f9b-4a1e-8100-d573c40312f8\"}]}")));

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

        //do not touch this
        Thread.sleep(10);

        verify(postRequestedFor(urlEqualTo("/cache?secondaryCache=yes")));
    }
}
