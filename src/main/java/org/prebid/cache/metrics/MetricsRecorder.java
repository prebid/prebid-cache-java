package org.prebid.cache.metrics;

import org.prebid.cache.handlers.ServiceType;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;

import java.time.Duration;
import java.util.Optional;

@Component
public class MetricsRecorder {

    private final MeterRegistry meterRegistry;

    protected static final String PREFIX_PLACEHOLDER = "\\$\\{prefix\\}";

    public MetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public enum MeasurementTag {
        REQUEST_DURATION("pbc.${prefix}.request.duration"),
        REQUEST("pbc.${prefix}.request"),
        ERROR_UNKNOWN("pbc.${prefix}.err.unknown"),
        ERROR_TIMEDOUT("pbc.${prefix}.err.timedOut"),
        ERROR_MISSINGID("pbc.${prefix}.err.missingId"),
        ERROR_BAD_REQUEST("pbc.${prefix}.err.badRequest"),
        REQUEST_INVALID("pbc.request.invalid"),
        JSON("pbc.${prefix}.json"),
        XML("pbc.${prefix}.xml"),
        ERROR_DB("pbc.${prefix}.err.db"),
        ERROR_SECONDARY_WRITE("pbc.err.secondaryWrite"),
        ERROR_EXISTINGID("pbc.err.existingId"),
        PROXY_SUCCESS("pbc.proxy.success"),
        PROXY_FAILURE("pbc.proxy.failure");

        @Getter
        private String tag;

        MeasurementTag(final String tag) {
            this.tag = tag;
        }
    }

    public class MetricsRecorderTimer {
        private Timer timer;
        private long start;
        private long stop;

        MetricsRecorderTimer(String measurementTag) {
            this.timer = meterRegistry.timer(measurementTag);
        }

        public MetricsRecorderTimer start() {
            this.start = System.currentTimeMillis();
            return this;
        }

        public void stop() {
            this.stop = System.currentTimeMillis();
            this.timer.record(Duration.ofMillis(this.stop - this.start));
        }
    }

    public Counter getInvalidRequestMeter() {
        return meterRegistry.counter(MeasurementTag.REQUEST_INVALID.getTag());
    }

    public Counter getSecondaryCacheWriteError() {
        return meterRegistry.counter(MeasurementTag.ERROR_SECONDARY_WRITE.getTag());
    }

    public Counter getExistingKeyError() {
        return meterRegistry.counter(MeasurementTag.ERROR_EXISTINGID.getTag());
    }

    public Counter getProxySuccess() {
        return meterRegistry.counter(MeasurementTag.PROXY_SUCCESS.getTag());
    }

    public Counter getProxyFailure() {
        return meterRegistry.counter(MeasurementTag.PROXY_FAILURE.getTag());
    }

    private Counter meterForTag(final String prefix, final MeasurementTag measurementTag) {
        return meterRegistry.counter(measurementTag.getTag().replaceAll(PREFIX_PLACEHOLDER, prefix));
    }

    public void markMeterForTag(final String prefix, final MeasurementTag measurementTag) {
        meterForTag(prefix, measurementTag).increment();
    }

    public Optional<MetricsRecorderTimer> createRequestContextTimerOptionalForServiceType(
            final ServiceType serviceType) {
        final MetricsRecorderTimer timer = getRequestTimerForServiceType(serviceType);
        if (timer != null)
            return Optional.of(timer.start());
        return Optional.empty();
    }

    private MetricsRecorderTimer getRequestTimerForServiceType(final ServiceType serviceType) {
        if (serviceType.equals(ServiceType.FETCH)) {
            return new MetricsRecorderTimer(
                MeasurementTag.REQUEST_DURATION.getTag()
                .replaceAll(PREFIX_PLACEHOLDER, "read"));
        } else if (serviceType.equals(ServiceType.SAVE)) {
            return new MetricsRecorderTimer(
                MeasurementTag.REQUEST_DURATION.getTag()
                .replaceAll(PREFIX_PLACEHOLDER, "write"));
        }
        return null;
    }
}
