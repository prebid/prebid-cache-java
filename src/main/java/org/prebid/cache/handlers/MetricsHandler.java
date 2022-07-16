package org.prebid.cache.handlers;

import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.metrics.MetricsRecorder;

abstract class MetricsHandler {
    MetricsRecorder metricsRecorder;
    PrebidServerResponseBuilder builder;
}
