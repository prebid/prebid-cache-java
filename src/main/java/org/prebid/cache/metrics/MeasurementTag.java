package org.prebid.cache.metrics;

import lombok.Getter;

@Getter
public enum MeasurementTag {
    REQUEST_DURATION("pbc.${prefix}.request.duration"),
    REQUEST("pbc.${prefix}.request"),
    REQUEST_TRUSTED("pbc.${prefix}.trustedRequest"),
    REQUEST_INVALID("pbc.request.invalid"),
    ERROR_UNKNOWN("pbc.${prefix}.err.unknown"),
    ERROR_TIMED_OUT("pbc.${prefix}.err.timedOut"),
    ERROR_MISSING_ID("pbc.${prefix}.err.missingId"),
    ERROR_BAD_REQUEST("pbc.${prefix}.err.badRequest"),
    ERROR_UNAUTHORIZED("pbc.${prefix}.err.unauthorized"),
    ERROR_DB("pbc.${prefix}.err.db"),
    JSON("pbc.${prefix}.json"),
    XML("pbc.${prefix}.xml"),
    ERROR_SECONDARY_WRITE("pbc.err.secondaryWrite"),
    ERROR_EXISTING_ID("pbc.err.existingId"),
    ERROR_REJECTED_EXTERNAL_ID("pbc.err.rejectedExternalId"),
    PROXY_SUCCESS("pbc.proxy.success"),
    PROXY_FAILURE("pbc.proxy.failure"),
    ENTRY_LIFETIME("pbc.entryLifetimeSec.${ttlBucket}");

    private final String tag;

    MeasurementTag(final String tag) {
        this.tag = tag;
    }
}
