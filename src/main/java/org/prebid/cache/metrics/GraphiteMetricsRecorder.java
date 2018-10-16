package org.prebid.cache.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.handlers.ServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GraphiteMetricsRecorder extends MetricsRecorder {
    protected static final String PREFIX_PLACEHOLDER = "\\$\\{prefix\\}";

    private static final MetricRegistry REGISTRY = new MetricRegistry();
    private final GraphiteConfig config;

    // GET endpoint metrics
    private static final Timer REQUEST_GET_DURATION = REGISTRY.timer(MeasurementTag.REQUEST_DURATION.getTag()
            .replaceAll(PREFIX_PLACEHOLDER, "read"));

    // POST endpoint metrics
    private static final Timer REQUEST_POST_DURATION = REGISTRY.timer(MeasurementTag.REQUEST_DURATION.getTag()
            .replaceAll(PREFIX_PLACEHOLDER, "write"));

    // Other 404
    private static final Meter INVALID_REQUEST_METER = REGISTRY.meter(MeasurementTag.REQUEST_INVALID.getTag());

    private static final Meter SECONDARY_CACHE_WRITE_ERROR =
            REGISTRY.meter(MeasurementTag.ERROR_SECONDARY_WRITE.getTag());

    private static final Meter EXISTING_KEY_ERROR = REGISTRY.meter(MeasurementTag.ERROR_EXISTINGID.getTag());

    @Autowired
    public GraphiteMetricsRecorder(final GraphiteConfig config) {
        this.config = config;
        startReport();
    }

    private void startReport() {
        if (!config.isEnabled())
            return;

        log.info("Starting {} host - [{}:{}].", GraphiteMetricsRecorder.class.getCanonicalName(), config.getHost(),
                config.getPort());
        final Graphite graphite = new Graphite(new InetSocketAddress(config.getHost(), config.getPort()));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(REGISTRY)
                .prefixedWith(config.getPrefix())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);
        reporter.start(1, TimeUnit.MINUTES);
    }

    public Meter getInvalidRequestMeter() {
        return INVALID_REQUEST_METER;
    }

    public Meter getSecondaryCacheWriteError() {
        return SECONDARY_CACHE_WRITE_ERROR;
    }

    public Meter getExistingKeyError() {
        return EXISTING_KEY_ERROR;
    }

    private Meter meterForTag(final String prefix, final MeasurementTag measurementTag) {
        return REGISTRY.meter(measurementTag.getTag().replaceAll(PREFIX_PLACEHOLDER, prefix));
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

    private Timer getRequestTimerForServiceType(final ServiceType serviceType) {
        if (serviceType.equals(ServiceType.FETCH)) {
            return REQUEST_GET_DURATION;
        } else if (serviceType.equals(ServiceType.SAVE)) {
            return REQUEST_POST_DURATION;
        }
        return null;
    }
}
