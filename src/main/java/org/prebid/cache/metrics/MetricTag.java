package org.prebid.cache.metrics;

public enum MetricTag {

    REQUEST_TYPE("requestType"),
    PAYLOAD_TYPE("payloadType"),
    ERROR("errorType"),
    PROXY("proxy");

    public final String tag;

    MetricTag(String tag) {
        this.tag = tag;
    }
}
