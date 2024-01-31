package org.prebid.cache.handlers;

import java.util.Arrays;

public enum PayloadType implements StringTypeConvertible {

    JSON("json"),
    XML("xml"),
    UNKNOWN("unknown");

    public final String type;

    PayloadType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static PayloadType from(String value) {
        return Arrays.stream(values())
            .filter(type -> type.type.equals(value))
            .findFirst()
            .orElse(UNKNOWN);
    }
}
