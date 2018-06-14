package org.prebid.cache.repository.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.ResultCode;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.helpers.Json;
import org.prebid.cache.listners.AerospikeReadListener;
import org.prebid.cache.listners.AerospikeWriteListener;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import reactor.core.publisher.Mono;
import reactor.retry.Retry;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class AerospikeRepositoryImpl implements ReactiveRepository<PayloadWrapper, String> {
    @NotNull
    private final AerospikePropertyConfiguration configuration;
    @NotNull
    private final AerospikeClient client;
    @NotNull
    private final EventLoops eventLoops;
    @NotNull
    private final Policy policy;
    private WritePolicy writePolicy;

    @Override
    public Mono save(final PayloadWrapper wrapper) {
        long expiry;
        String normalizedId;
        WritePolicy policy = writePolicy();

        try {
            expiry = wrapper.getExpiry();
            normalizedId = wrapper.getNormalizedId();
            policy.expiration = (int) expiry;
        } catch (PayloadWrapperPropertyException e) {
            e.printStackTrace();
            return Mono.empty();
        }

        return Mono.<String>create(sink -> client.put(eventLoops.next(),
                new AerospikeWriteListener(sink, normalizedId), policy,
                new Key(configuration.getNamespace(), "", normalizedId),
                new Bin(configuration.getBinName(), Json.toJson(wrapper)))).map(payload -> wrapper)
                .retryWhen(getRetryPolicy());
    }

    @Override
    public Mono<PayloadWrapper> findById(String id) {
        return Mono.<String>create(sink -> client.get(eventLoops.next(),
                new AerospikeReadListener(sink, id),
                policy, new Key(configuration.getNamespace(), "", id)))
                .map(json -> Json.createPayloadFromJson(json, PayloadWrapper.class))
                .retryWhen(getRetryPolicy());
    }

    private WritePolicy writePolicy() {
        if (Objects.isNull(writePolicy)) {
            writePolicy = new WritePolicy();
        }
        return writePolicy;
    }

    private List<Integer> getRetryCodes() {
        return Arrays.asList(ResultCode.GENERATION_ERROR, ResultCode.KEY_EXISTS_ERROR, ResultCode.KEY_NOT_FOUND_ERROR);
    }

    private Retry<Object> getRetryPolicy() {
        Duration firstBackoff = Duration.ofMillis(configuration.getFirstBackoff());
        Duration maxBackoff = Duration.ofMillis(configuration.getMaxBackoff());

        return Retry.onlyIf(context -> context.exception() instanceof AerospikeException
                && getRetryCodes().contains(((AerospikeException) context.exception()).getResultCode())
        ).doOnRetry(context -> log.warn("Retrying context {}", context))
                .retryMax(configuration.getMaxRetry())
                .exponentialBackoffWithJitter(firstBackoff, maxBackoff);
    }
}
