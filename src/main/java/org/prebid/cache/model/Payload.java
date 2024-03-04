package org.prebid.cache.model;

import lombok.Value;

@Value(staticConstructor = "of")
public class Payload {
    String type;
    String key;
    String value;
}
