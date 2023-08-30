package org.prebid.cache.repository;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CircuitBreakerSecuredReactiveRepository<T, R> implements ReactiveRepository<T, R> {

    private final ReactiveRepository<T, R> delegate;
    private final CircuitBreaker circuitBreaker;

    @Override
    public Mono<T> save(T wrapper) {
        return delegate.save(wrapper)
            .transform(CircuitBreakerOperator.of(circuitBreaker));
    }

    @Override
    public Mono<T> findById(R id) {
        return delegate.findById(id)
            .transform(CircuitBreakerOperator.of(circuitBreaker));
    }
}
