package org.prebid.cache.repository;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class CircuitBreakerSecuredReactiveRepository<T, R> implements ReactiveRepository<T, R> {

    private final ReactiveRepository<T, R> delegate;
    private final CircuitBreaker circuitBreaker;

    @Override
    public Mono<T> save(T wrapper) {
        return delegate.save(wrapper)
                .doOnError(error -> log.error("Error while accessing data source: {}", error.getMessage(), error))
                .transform(CircuitBreakerOperator.of(circuitBreaker));
    }

    @Override
    public Mono<T> findById(R id) {
        return delegate.findById(id)
                .doOnError(error -> log.error("Error while accessing data source: {}", error.getMessage(), error))
                .transform(CircuitBreakerOperator.of(circuitBreaker));
    }
}
