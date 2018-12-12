package org.prebid.cache.exceptions;

public class RequestBodyDeserializeException extends PrebidException {
    public RequestBodyDeserializeException(String message, Throwable t) {
        super(message, t);
    }
}
