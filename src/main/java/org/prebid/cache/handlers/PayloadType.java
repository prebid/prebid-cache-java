package org.prebid.cache.handlers;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PayloadType {

    JSON("json"),
    XML("xml"),
    TEXT("text");

    @JsonValue
    private final String text;

    PayloadType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
