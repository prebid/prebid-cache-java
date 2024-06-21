package org.prebid.cache.model;

import lombok.Builder;
import lombok.Value;
import org.prebid.cache.handlers.PayloadType;

@Value
@Builder
public class ModulePayload {

    public String key;

    public PayloadType type;

    public String value;

    public String application;

    public Integer ttlseconds;
}
