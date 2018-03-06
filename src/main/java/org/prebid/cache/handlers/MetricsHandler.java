package org.prebid.cache.handlers;

import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.metrics.GraphiteMetricsRecorder;

abstract class MetricsHandler {
    GraphiteMetricsRecorder metricsRecorder;
    PrebidServerResponseBuilder builder;
}
