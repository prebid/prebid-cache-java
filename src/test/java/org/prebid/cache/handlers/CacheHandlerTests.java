package org.prebid.cache.handlers;

import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.handlers.cache.CacheHandler;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadTransfer;
import org.prebid.cache.model.PayloadWrapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class CacheHandlerTests {

    static final PayloadTransfer PAYLOAD_TRANSFER = PayloadTransfer.builder()
            .type("json")
            .key("2be04ba5-8f9b-4a1e-8100-d573c40312f8")
            .value("")
            .expiry(1800L)
            .prefix("prebid_")
            .build();

    static final PayloadWrapper PAYLOAD_WRAPPER = PayloadWrapper.builder()
            .id("2be04ba5-8f9b-4a1e-8100-d573c40312f8")
            .prefix("prebid_")
            .payload(Payload.of("json", "2be04ba5-8f9b-4a1e-8100-d573c40312f8", ""))
            .expiry(1800L)
            .isExternalId(true)
            .build();

    void verifyRepositoryError(CacheHandler handler) {
        final Consumer<Throwable> consumer = t -> assertTrue(t instanceof RepositoryException);
        verifyResultTest(consumer, handler);
    }

    void verifyJacksonError(CacheHandler handler) {
        final Consumer<Throwable> consumer = t -> assertTrue(t instanceof RequestParsingException);
        verifyResultTest(consumer, handler);
    }

    private void verifyResultTest(Consumer<Throwable> consumer, CacheHandler handler) {
        Mono<Throwable> error =
                handler.validateErrorResult(Mono.error(new Exception("jackson error")));

        error.doOnError(consumer)
                .subscribe(consumer);
        StepVerifier.create(error)
                .expectSubscription()
                .expectError()
                .verify();
    }
}
