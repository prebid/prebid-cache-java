package org.prebid.cache.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.Value;

import java.util.*;

@Value
@Builder
@RequiredArgsConstructor
public class RequestObject {
    @NonNull
    @Singular
    private List<PayloadTransfer> puts;
}
