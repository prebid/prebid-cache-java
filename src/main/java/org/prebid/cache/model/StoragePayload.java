package org.prebid.cache.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.prebid.cache.handlers.PayloadType;

@Value
@Builder
public class StoragePayload {

    @NotEmpty
    public String key;

    @NotNull
    public PayloadType type;

    @NotEmpty
    public String value;

    @NotEmpty
    public String application;

    @Min(0)
    public Integer ttlseconds;
}
