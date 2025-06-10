package org.prebid.cache.repository.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.ResultCode;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.helpers.Json;
import org.prebid.cache.listeners.AerospikeReadListener;
import org.prebid.cache.listeners.AerospikeWriteListener;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

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

    private static final String BIN_NAME = "cache";

    @Override
    public Mono<PayloadWrapper> save(final PayloadWrapper wrapper) {
        long expiry;
        String normalizedId;
        WritePolicy policy = writePolicy();

        try {
            expiry = wrapper.getExpiry();
            normalizedId = wrapper.getNormalizedId();
            policy.expiration = (int) expiry;
        } catch (PayloadWrapperPropertyException e) {
            log.error("Exception occurred while extracting normalized id from payload: '{}', cause: '{}'",
                    ExceptionUtils.getMessage(e), ExceptionUtils.getMessage(e));
            return Mono.empty();
        }

        return Mono.<String>create(sink -> client.put(eventLoops.next(),
                        new AerospikeWriteListener(sink, normalizedId), policy,
                        new Key(configuration.getNamespace(), "", normalizedId),
                        new Bin(BIN_NAME, Json.toJson(wrapper)))).map(payload -> wrapper)
                .retryWhen(getRetryPolicy())
                .onErrorResume(this::handleAerospikeError);
    }

    @Override
    public Mono<PayloadWrapper> findById(String id) {
        return Mono.<String>create(sink -> client.get(eventLoops.next(),
                        new AerospikeReadListener(sink, id),
                        policy, new Key(configuration.getNamespace(), "", id)))
                .map(json -> Json.createPayloadFromJson(json, PayloadWrapper.class))
                .retryWhen(getRetryPolicy())
                .onErrorResume(this::handleAerospikeError);
    }

    private WritePolicy writePolicy() {
        final WritePolicy writePolicy = new WritePolicy();
        writePolicy.setConnectTimeout(configuration.getConnectTimeout());
        writePolicy.setTimeouts(configuration.getSocketTimeout(), configuration.getTotalTimeout());
        if (configuration.isPreventUUIDDuplication()) {
            writePolicy.recordExistsAction = RecordExistsAction.CREATE_ONLY;
        }
        return writePolicy;
    }

    private List<Integer> getRetryCodes() {
        return Arrays.asList(ResultCode.GENERATION_ERROR, ResultCode.KEY_NOT_FOUND_ERROR);
    }

    private Retry getRetryPolicy() {
        Duration minBackoff = Duration.ofMillis(configuration.getFirstBackoff());
        Duration maxBackoff = Duration.ofMillis(configuration.getMaxBackoff());
        long maxAttempts = configuration.getMaxRetry();

        return Retry.backoff(maxAttempts, minBackoff)
                .maxBackoff(maxBackoff)
                .filter(e -> e instanceof AerospikeException
                        && getRetryCodes().contains(((AerospikeException) e).getResultCode()))
                .doAfterRetry(signal -> log.warn("Retrying context {}", signal.retryContextView()));
    }

    private <T> Mono<T> handleAerospikeError(Throwable throwable) {
        if (throwable instanceof AerospikeException aerospikeException) {
            if (aerospikeException.getResultCode() == ResultCode.KEY_EXISTS_ERROR) {
                return Mono.error(new DuplicateKeyException(throwable.toString(), throwable));
            }

            return Mono.error(new RepositoryException(throwable.toString(), throwable));
        }

        return Mono.error(throwable);
    }
}
