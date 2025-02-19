package org.prebid.cache.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.prebid.cache.handlers.ServiceType;
import org.springframework.stereotype.Component;

@Component
public class MetricsRecorder {

    private final MeterRegistry meterRegistry;

    protected static final String PREFIX_PLACEHOLDER = "\\$\\{prefix\\}";

    public MetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public class MetricsRecorderTimer {
        private final Timer timer;
        private final Timer.Sample sample;

        MetricsRecorderTimer(String measurementTag) {
            timer = meterRegistry.timer(measurementTag);
            sample = Timer.start(meterRegistry);
        }

        public void stop() {
            sample.stop(timer);
        }
    }

    public Counter getInvalidRequestMeter() {
        return meterRegistry.counter(MeasurementTag.REQUEST_INVALID.getTag());
    }

    public Counter getSecondaryCacheWriteError() {
        return meterRegistry.counter(MeasurementTag.ERROR_SECONDARY_WRITE.getTag());
    }

    public Counter getExistingKeyError() {
        return meterRegistry.counter(MeasurementTag.ERROR_EXISTING_ID.getTag());
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

    public MetricsRecorderTimer createRequestTimerForServiceType(final ServiceType serviceType) {
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
