package org.prebid.cache.metrics;

import lombok.Getter;
import lombok.Setter;

public abstract class MetricsRecorder
{
    public enum MeasurementTag {
        REQUEST_DURATION("request_duration"),
        REQUEST_RATE("request_rate"),
        ERROR_RATE("error_rate"),
        RESOURCE_NOT_FOUND_RATE("resource-not-found_rate"),
        BAD_REQUEST_RATE("bad-request_rate"),
        INVALID_REQUEST_RATE("invalid-request_rate"),
        JSON_REQUEST_RATE("json-request_rate"),
        XML_REQUEST_RATE("xml-request_rate");

        @Getter @Setter private String tag;

        MeasurementTag(final String tag) {
            this.tag = tag;
        }
    }
}
