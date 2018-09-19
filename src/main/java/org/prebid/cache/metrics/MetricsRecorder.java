package org.prebid.cache.metrics;

import lombok.Getter;
import lombok.Setter;

public abstract class MetricsRecorder
{
    public enum MeasurementTag {
        REQUEST_DURATION("pbc.${prefix}.request.duration"),
        REQUEST("pbc.${prefix}.request"),
        ERROR_UNKNOWN("pbc.${prefix}.err.unknown"),
        ERROR_MISSINGID("pbc.${prefix}.err.missingId"),
        ERROR_BAD_REQUEST("pbc.${prefix}.err.badRequest"),
        REQUEST_INVALID("pbc.request.invalid"),
        JSON("pbc.${prefix}.json"),
        XML("pbc.${prefix}.xml"),
        ERROR_DB("pbc.${prefix}.err.db");

        @Getter private String tag;

        MeasurementTag(final String tag) {
            this.tag = tag;
        }
    }
}
