package org.prebid.cache.model;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class ErrorResponse {
    String error;
    int status;
    String path;
    String message;
    Date timestamp;
}
