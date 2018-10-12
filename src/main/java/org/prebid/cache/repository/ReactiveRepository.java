package org.prebid.cache.repository;

import reactor.core.publisher.Mono;

public interface ReactiveRepository<T, R> {
    Mono<T> save(T wrapper);

    Mono<T> findById(R id);
}
