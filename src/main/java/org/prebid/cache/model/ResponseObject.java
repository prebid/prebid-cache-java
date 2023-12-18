package org.prebid.cache.model;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value(staticConstructor = "of")
public class ResponseObject {
    List<Map<String, String>> responses;
}
