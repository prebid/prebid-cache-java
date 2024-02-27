package org.prebid.cache.model;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.prebid.cache.exceptions.PayloadWrapperPropertyException;

@Value
@Builder
@RequiredArgsConstructor
public class PayloadWrapper {
    String id;
    String prefix;
    Payload payload;

    Long expiry;

    transient boolean isExternalId;

    public String getNormalizedId() throws PayloadWrapperPropertyException {
        if (prefix != null || id != null) {
            return String.format("%s%s", prefix, id);
        } else {
            throw new PayloadWrapperPropertyException("Invalid Id or Prefix.");
        }
    }
}
