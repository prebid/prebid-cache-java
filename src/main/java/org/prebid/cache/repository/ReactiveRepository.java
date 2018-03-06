package org.prebid.cache.repository;

import reactor.core.publisher.Mono;

public interface ReactiveRepository <T, R>
{
    Mono<T> save(final T wrapper);
    Mono<T> findById(final R id);
}
