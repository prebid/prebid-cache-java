package org.prebid.cache.metrics;

public enum MetricType {

    DURATION("duration"),
    COUNT("count");

    public final String value;

    MetricType(String value) {
        this.value = value;
    }
}
