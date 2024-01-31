package org.prebid.cache.metrics;

public enum ErrorType {

    UNKNOWN("unknown"),
    TIMED_OUT("timedOut"),
    MISSING_ID("missingId"),
    BAD_REQUEST("badRequest"),
    SECONDARY_WRITE("secondaryWrite"),
    EXISTING_ID("existingId"),
    DATABASE_ERROR("db");

    public final String value;

    ErrorType(String value) {
        this.value = value;
    }
}
