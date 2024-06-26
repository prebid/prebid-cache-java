package org.prebid.cache.handlers;

import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.metrics.MetricsRecorder;

public abstract class MetricsHandler {
    protected MetricsRecorder metricsRecorder;
    protected PrebidServerResponseBuilder builder;
}
