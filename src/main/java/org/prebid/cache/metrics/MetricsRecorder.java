package org.prebid.cache.metrics;

import lombok.Getter;
import lombok.Setter;

public abstract class MetricsRecorder
{
    public enum MeasurementTag {
        REQUEST_DURATION("pbc.prefix.request.duration"),
        REQUEST_RATE("pbc.prefix.request"),
        ERROR_RATE("pbc.prefix.err.unknown"),
        ERROR_MISSINGID_RATE("pbc.prefix.err.missingId"),
        ERROR_BAD_REQUEST_RATE("pbc.prefix.err.badRequest"),
        REQUEST_INVALID_RATE("request.invalid"),
        JSON_RATE("pbc.prefix.json"),
        XML_RATE("pbc.prefix.xml"),
        SYSTEM_ERR_RATE("pbc.prefix.err.db");

        @Getter @Setter private String tag;

        MeasurementTag(final String tag) {
            this.tag = tag;
        }
    }
}
