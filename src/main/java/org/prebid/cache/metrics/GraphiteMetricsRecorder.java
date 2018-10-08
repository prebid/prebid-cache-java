package org.prebid.cache.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.prebid.cache.handlers.ServiceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class GraphiteMetricsRecorder extends MetricsRecorder
{
    protected static final String PREFIX_PLACEHOLDER = "\\$\\{prefix\\}";

    final private static MetricRegistry registry = new MetricRegistry();
    final private GraphiteConfig config;

    // GET endpoint metrics
    private static final Timer requestGetDuration = registry.timer(MeasurementTag.REQUEST_DURATION.getTag()
            .replaceAll(PREFIX_PLACEHOLDER, "read"));

    // POST endpoint metrics
    private static final Timer requestPostDuration = registry.timer(MeasurementTag.REQUEST_DURATION.getTag()
            .replaceAll(PREFIX_PLACEHOLDER, "write"));

    // Other 404
    private static final Meter invalidRequestMeter = registry.meter(MeasurementTag.REQUEST_INVALID.getTag());

    private static final Meter secondaryCacheWriteError = registry.meter(MeasurementTag.ERROR_SECONDARY_WRITE.getTag());

    private static final Meter existingKeyError = registry.meter(MeasurementTag.ERROR_EXISTINGID.getTag());

    @Autowired
    public GraphiteMetricsRecorder(final GraphiteConfig config) {
        this.config = config;
        startReport();
    }

    private void startReport() {
        if (!config.isEnabled())
            return;

        log.info("Starting {} host - [{}:{}].", GraphiteMetricsRecorder.class.getCanonicalName(), config.getHost(), config.getPort());
        final Graphite graphite = new Graphite(new InetSocketAddress(config.getHost(), config.getPort()));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(registry)
                .prefixedWith(config.getPrefix())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);
        reporter.start(1, TimeUnit.MINUTES);
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

    private Meter meterForTag(final String prefix, final MeasurementTag measurementTag) {
        return registry.meter(measurementTag.getTag().replaceAll(PREFIX_PLACEHOLDER, prefix));
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
            return requestGetDuration;
        } else if (serviceType.equals(ServiceType.SAVE)) {
            return requestPostDuration;
        }
        return null;
    }
}
