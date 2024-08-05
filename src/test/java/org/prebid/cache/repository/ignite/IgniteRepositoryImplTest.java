package org.prebid.cache.repository.ignite;

import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.internal.client.thin.IgniteClientFutureImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IgniteRepositoryImplTest {

    @Mock
    private ClientCache<String, String> cache;

    private IgniteRepositoryImpl target;

    @BeforeEach
    public void before() {
        target = new IgniteRepositoryImpl(cache);
    }

    @Test
    public void findByIdShouldSuccessfullyReturnPayloadWrapper() {
        // given
        final String givenPayload = """
                {
                    "id": "key",
                    "prefix": "",
                    "payload": {
                        "type": "text",
                        "key": "key",
                        "value": "value"
                    },
                    "expiry": 999
                }
                """;

        given(cache.getAsync("key")).willReturn(IgniteClientFutureImpl.completedFuture(givenPayload));

        // when
        final Mono<PayloadWrapper> result = target.findById("key");

        // then
        final PayloadWrapper expectedPayload = PayloadWrapper.builder()
                .id("key")
                .prefix("")
                .payload(Payload.of("text", "key", "value"))
                .expiry(999L)
                .build();

        StepVerifier.create(result)
                .consumeNextWith(actualPayload -> assertThat(actualPayload).isEqualTo(expectedPayload))
                .expectComplete()
                .verify();
    }

    @Test
    public void findByIdShouldFailWithClientException() {
        // given
        given(cache.getAsync("key")).willReturn(new IgniteClientFutureImpl<>(
                CompletableFuture.failedFuture(new ClientException("something went wrong"))));

        // when
        final Mono<PayloadWrapper> result = target.findById("key");

        // then
        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(RepositoryException.class);
                    assertThat(throwable.getMessage()).hasSameClassAs("something went wrong");
                })
                .verify();
    }

    @Test
    public void saveShouldSuccessfullySavePayload() {
        // given
        final String givenPayload = """
                {
                    "id": "key",
                    "prefix": "",
                    "payload": {
                        "type": "text",
                        "key": "key",
                        "value": "value"
                    },
                    "expiry": 999
                }
                """.replaceAll("\\s+", "");

        given(cache.withExpirePolicy(any())).willAnswer(ignored -> cache);
        given(cache.putIfAbsentAsync("key", givenPayload)).willReturn(IgniteClientFutureImpl.completedFuture(true));


        // when
        final PayloadWrapper givenPayloadWrapper = PayloadWrapper.builder()
                .id("key")
                .prefix("")
                .payload(Payload.of("text", "key", "value"))
                .expiry(999L)
                .build();

        final Mono<PayloadWrapper> result = target.save(givenPayloadWrapper);

        // then
        StepVerifier.create(result)
                .consumeNextWith(actualPayload -> assertThat(actualPayload).isEqualTo(givenPayloadWrapper))
                .expectComplete()
                .verify();

        final CreatedExpiryPolicy expectedPolicy = new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS, 999));
        verify(cache).withExpirePolicy(eq(expectedPolicy));
    }

    @Test
    public void saveShouldFailWithClientException() {
        // given
        final String givenPayload = """
                {
                    "id": "key",
                    "prefix": "",
                    "payload": {
                        "type": "text",
                        "key": "key",
                        "value": "value"
                    },
                    "expiry": 999
                }
                """.replaceAll("\\s+", "");

        given(cache.withExpirePolicy(any())).willAnswer(ignored -> cache);
        given(cache.putIfAbsentAsync("key", givenPayload)).willReturn(new IgniteClientFutureImpl<>(
                CompletableFuture.failedFuture(new ClientException("something went wrong"))));


        // when
        final PayloadWrapper givenPayloadWrapper = PayloadWrapper.builder()
                .id("key")
                .prefix("")
                .payload(Payload.of("text", "key", "value"))
                .expiry(999L)
                .build();

        final Mono<PayloadWrapper> result = target.save(givenPayloadWrapper);

        // then
        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(RepositoryException.class);
                    assertThat(throwable.getMessage()).hasSameClassAs("something went wrong");
                })
                .verify();
    }

    @Test
    public void saveShouldReturnEmptyResultWhenKeyAndPrefixAreAbsent() {
        // when
        final PayloadWrapper givenPayloadWrapper = PayloadWrapper.builder()
                .payload(Payload.of("text", "key", "value"))
                .expiry(999L)
                .build();

        final Mono<PayloadWrapper> result = target.save(givenPayloadWrapper);

        // then
        StepVerifier.create(result).verifyComplete();
        verifyNoInteractions(cache);
    }
}
