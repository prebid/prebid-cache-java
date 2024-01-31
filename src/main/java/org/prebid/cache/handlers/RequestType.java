package org.prebid.cache.handlers;

public enum RequestType implements StringTypeConvertible {

    FETCH("read"), SAVE("write"), UNKNOWN("unknown");

    public final String value;

    RequestType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
