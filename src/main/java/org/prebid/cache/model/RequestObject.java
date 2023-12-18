package org.prebid.cache.model;

import lombok.Value;

import java.util.List;

@Value(staticConstructor = "of")
public class RequestObject {
    List<PayloadTransfer> puts;
}
