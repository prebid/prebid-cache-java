package org.prebid.cache.handlers;

import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.RepositoryException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class CacheHandlerTests {

    void verifyRepositoryError(CacheHandler handler) {
        final Consumer<Throwable> consumer = (t) -> {
            assertTrue(t instanceof RepositoryException);
        };

        verifyResultTest(consumer, handler);
    }

    void verifyJacksonError(CacheHandler handler) {
        final Consumer<Throwable> consumer = (t) -> {
            assertTrue(t instanceof RequestParsingException);
        };

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
