package org.prebid.cache.repository.module.storage;

import lombok.RequiredArgsConstructor;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class ModuleCompositeRepository {

    private final Map<String, ReactiveRepository<PayloadWrapper, String>> applicationToSource;

    public Mono<PayloadWrapper> save(String application, PayloadWrapper wrapper) {
        return Optional.ofNullable(application)
                .map(applicationToSource::get)
                .map(source -> source.save(wrapper))
                .orElse(Mono.error(new ResourceNotFoundException("Invalid application: " + application)));
    }

    public Mono<PayloadWrapper> findById(String application, String id) {
        return Optional.ofNullable(application)
                .map(applicationToSource::get)
                .map(source -> source.findById(id))
                .orElse(Mono.error(new ResourceNotFoundException("Invalid application: " + application)));
    }
}
