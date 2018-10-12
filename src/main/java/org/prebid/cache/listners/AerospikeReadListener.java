package org.prebid.cache.listners;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.listener.RecordListener;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.MonoSink;

import java.util.Objects;

@Slf4j
public class AerospikeReadListener implements RecordListener {
    private static final String NAME = "cache";
    private final MonoSink<String> sink;
    private final String recordKeyId;

    public AerospikeReadListener(MonoSink<String> sink, String recordKeyId) {
        this.sink = sink;
        this.recordKeyId = recordKeyId;
    }

    @Override
    public void onSuccess(Key key, Record record) {
        if (Objects.nonNull(record)) {
            sink.success(record.getString(NAME));
        } else {
            sink.success();
        }
    }

    @Override
    public void onFailure(AerospikeException exception) {
        log.error("Error when reading record with keyId {}", recordKeyId);
        sink.error(exception);
    }
}
