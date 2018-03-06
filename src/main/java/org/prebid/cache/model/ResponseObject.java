package org.prebid.cache.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.Value;
import java.util.List;
import java.util.Map;

@Value
@Builder
@RequiredArgsConstructor
public class ResponseObject{
    @NonNull
    @Singular
    List<Map<String, String>> responses;
}
