package org.prebid.cache.metrics;

import lombok.Getter;
import lombok.Setter;

public abstract class MetricsRecorder
{
    public enum MeasurementTag {
        REQUEST_DURATION("pbc.prefix.request.duration"),
        REQUEST_RATE("pbc.prefix.request.rate"),
        ERROR_RATE("pbc.prefix.err.rate"),
        ERROR_MISSINGID("pbc.prefix.err.missingId"),
        ERROR_BAD_REQUEST("pbc.prefix.err.badRequest"),
        REQUEST_INVALID_RATE("request.invalid.rate"),
        JSON_RATE("pbc.prefix.json.rate"),
        XML_RATE("pbc.prefix.xml.rate"),
        SYSTEM_ERR_RATE("pbc.prefix.err.systemErr");

        @Getter @Setter private String tag;

        MeasurementTag(final String tag) {
            this.tag = tag;
        }
    }
}
