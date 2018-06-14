package org.prebid.cache.listners;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.listener.WriteListener;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.MonoSink;

@Slf4j
public class AerospikeWriteListener implements WriteListener {
    private final MonoSink<String> sink;
    private final String keyId;

    public AerospikeWriteListener(MonoSink<String> sink, String keyId) {
        this.sink = sink;
        this.keyId = keyId;
    }

    @Override
    public void onSuccess(Key key) {
        sink.success(key.userKey.toString());
    }

    @Override
    public void onFailure(AerospikeException exception) {
        log.error("Error when writing record with keyID : {}", keyId);
        sink.error(exception);
    }
}
