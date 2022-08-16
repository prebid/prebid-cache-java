package org.prebid.cache.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.prebid.cache.helpers.Json;

@Value
@Builder(toBuilder = true)
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayloadTransfer {
    String type;
    String key;
    Object value;
    Long expiry;
    Long ttlseconds;
    transient String prefix;

    public String valueAsString() {
        if (value == null)
            return null;

        if (value instanceof String) {
            return value.toString();
        } else {
            return Json.toJson(value);
        }
    }

    public Long compareAndGetExpiry() {
        return ttlseconds != null ? ttlseconds : expiry;
    }
}
