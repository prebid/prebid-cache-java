package org.prebid.cache.repository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RequiredArgsConstructor
public class TimeOutCapableReactiveRepository<T, R> implements ReactiveRepository<T, R> {

    private final ReactiveRepository<T, R> delegate;

    private final Duration duration;

    @Override
    public Mono<T> save(T wrapper) {
        return delegate.save(wrapper)
            .timeout(duration);
    }

    @Override
    public Mono<T> findById(R id) {
        return delegate.findById(id)
            .timeout(duration);
    }
}
