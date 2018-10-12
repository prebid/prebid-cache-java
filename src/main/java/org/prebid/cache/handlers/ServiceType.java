package org.prebid.cache.handlers;

public enum ServiceType implements StringTypeConvertible {
    SAVE("POST"),
    FETCH("GET");

    private final String text;

    ServiceType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
