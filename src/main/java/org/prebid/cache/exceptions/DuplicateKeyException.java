package org.prebid.cache.exceptions;

public class DuplicateKeyException extends PrebidException {
    public DuplicateKeyException(String message) {
        super(message);
    }

    public DuplicateKeyException(String message, Throwable t) {
        super(message, t);
    }
}
