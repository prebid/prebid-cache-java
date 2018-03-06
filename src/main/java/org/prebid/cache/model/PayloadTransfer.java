package org.prebid.cache.model;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.prebid.cache.helpers.Json;


@Value
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class PayloadTransfer {
    String type;
    String key;
    Object value;
    Long expiry;
    transient String prefix;

    public String getValue() {
        if (value == null)
            return null;

        if (value instanceof String) {
            return value.toString();
        } else {
            return Json.toJson(value);
        }
    }
}
