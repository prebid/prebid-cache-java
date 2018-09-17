package org.prebid.cache.model;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PayloadTransfer that = (PayloadTransfer) o;

        return new EqualsBuilder()
                .append(type, that.type)
                .append(key, that.key)
                .append(value, that.value)
                .append(expiry, that.expiry)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(type)
                .append(key)
                .append(value)
                .append(expiry)
                .toHashCode();
    }
}
