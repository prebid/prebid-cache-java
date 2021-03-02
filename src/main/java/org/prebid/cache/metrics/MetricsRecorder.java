package org.prebid.cache.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import lombok.Getter;
import org.prebid.cache.handlers.ServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MetricsRecorder {

    protected static final String PREFIX_PLACEHOLDER = "\\$\\{prefix\\}";

    private final MetricRegistry metricRegistry;

    // GET endpoint metrics
    private final Timer requestGetDuration;

    // POST endpoint metrics
    private final Timer requestPostDuration;
    // Other 404
    private final Meter invalidRequestMeter;

    private final Meter secondaryCacheWriteError;

    private final Meter existingKeyError;

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
        ERROR_EXISTINGID("pbc.err.existingId");

        @Getter
        private String tag;

        MeasurementTag(final String tag) {
            this.tag = tag;
        }
    }

    @Autowired
    public MetricsRecorder(MetricRegistry metricRegistry) {
        this.metricRegistry = new MetricRegistry();
        this.requestGetDuration = metricRegistry.timer(MeasurementTag.REQUEST_DURATION.getTag()
                .replaceAll(PREFIX_PLACEHOLDER, "read"));
        this.requestPostDuration = metricRegistry.timer(MeasurementTag.REQUEST_DURATION.getTag()
                .replaceAll(PREFIX_PLACEHOLDER, "write"));

        this.secondaryCacheWriteError = metricRegistry.meter(MeasurementTag.ERROR_SECONDARY_WRITE.getTag());
        this.invalidRequestMeter = metricRegistry.meter(MeasurementTag.REQUEST_INVALID.getTag());
        this.existingKeyError = metricRegistry.meter(MeasurementTag.ERROR_EXISTINGID.getTag());
    }

    public Meter getInvalidRequestMeter() {
        return invalidRequestMeter;
    }

    public Meter getSecondaryCacheWriteError() {
        return secondaryCacheWriteError;
    }

    public Meter getExistingKeyError() {
        return existingKeyError;
    }

    public void markMeterForTag(final String prefix, final MeasurementTag measurementTag) {
        meterForTag(prefix, measurementTag).mark();
    }

    public Optional<Timer.Context> createRequestContextTimerOptionalForServiceType(final ServiceType serviceType) {
        final Timer timer = getRequestTimerForServiceType(serviceType);
        if (timer != null)
            return Optional.of(timer.time());
        return Optional.empty();
    }

    private Meter meterForTag(final String prefix, final MeasurementTag measurementTag) {
        return metricRegistry.meter(measurementTag.getTag().replaceAll(PREFIX_PLACEHOLDER, prefix));
    }

    private Timer getRequestTimerForServiceType(final ServiceType serviceType) {
        if (serviceType.equals(ServiceType.FETCH)) {
            return requestGetDuration;
        } else if (serviceType.equals(ServiceType.SAVE)) {
            return requestPostDuration;
        }
        return null;
    }

}
