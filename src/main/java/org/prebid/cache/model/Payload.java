package org.prebid.cache.model;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class Payload {
    String type;
    String key;
    String value;
}
