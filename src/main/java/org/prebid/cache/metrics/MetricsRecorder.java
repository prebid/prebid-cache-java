package org.prebid.cache.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.prebid.cache.handlers.PayloadType;
import org.prebid.cache.handlers.RequestType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MetricsRecorder {

    private final MeterRegistry meterRegistry;

    public MetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public class RequestDurationRecorder {
        private Timer timer;
        private Timer.Sample sample;

        RequestDurationRecorder(RequestType requestType) {
            timer = meterRegistry.timer(MetricType.DURATION.value, MetricTag.REQUEST_TYPE.tag, requestType.value);
            sample = Timer.start(meterRegistry);
        }

        public void stop() {
            sample.stop(timer);
        }
    }

    public void incrementInvalidRequestCount() {
        final List<Tag> tags = List.of(
            Tag.of(MetricTag.REQUEST_TYPE.tag, RequestType.UNKNOWN.value),
            Tag.of(MetricTag.ERROR.tag, ErrorType.BAD_REQUEST.value));

        meterRegistry.counter(MetricType.COUNT.value, tags).increment();
    }

    public void incrementSecondaryCacheWriteErrorCount() {
        final List<Tag> tags = List.of(
            Tag.of(MetricTag.REQUEST_TYPE.tag, RequestType.SAVE.value),
            Tag.of(MetricTag.ERROR.tag, ErrorType.SECONDARY_WRITE.value));

        meterRegistry.counter(MetricType.COUNT.value, tags).increment();
    }

    public void incrementExistingKeyErrorCount() {
        final List<Tag> tags = List.of(
            Tag.of(MetricTag.REQUEST_TYPE.tag, RequestType.SAVE.value),
            Tag.of(MetricTag.ERROR.tag, ErrorType.EXISTING_ID.value));

        meterRegistry.counter(MetricType.COUNT.value, tags).increment();
    }

    public void incrementProxySuccessCount() {
        final List<Tag> tags = List.of(
            Tag.of(MetricTag.REQUEST_TYPE.tag, RequestType.FETCH.value),
            Tag.of(MetricTag.PROXY.tag, ProxyStatus.SUCCESS.status));

        meterRegistry.counter(MetricType.COUNT.value, tags).increment();
    }

    public void incrementProxyFailureCount() {
        final List<Tag> tags = List.of(
            Tag.of(MetricTag.REQUEST_TYPE.tag, RequestType.FETCH.value),
            Tag.of(MetricTag.PROXY.tag, ProxyStatus.FAILURE.status));

        meterRegistry.counter(MetricType.COUNT.value, tags).increment();
    }

    public void incrementErrorCount(RequestType requestType, ErrorType errorType) {
        final List<Tag> tags = List.of(
            Tag.of(MetricTag.REQUEST_TYPE.tag, requestType.value),
            Tag.of(MetricTag.ERROR.tag, errorType.value));

        meterRegistry.counter(MetricType.COUNT.value, tags).increment();
    }

    public void incrementRequestCount(RequestType requestType) {
        meterRegistry.counter(MetricType.COUNT.value, MetricTag.REQUEST_TYPE.tag, requestType.value).increment();
    }

    public void incrementResponseCount(RequestType requestType, PayloadType payloadType) {
        final List<Tag> tags = List.of(
            Tag.of(MetricTag.REQUEST_TYPE.tag, requestType.value),
            Tag.of(MetricTag.PAYLOAD_TYPE.tag, payloadType.type));

        meterRegistry.counter(MetricType.COUNT.value, tags).increment();
    }

    public RequestDurationRecorder createRequestTimer(RequestType requestType) {
        return new RequestDurationRecorder(requestType);
    }
}
