package org.prebid.cache.exceptions;

public abstract class PrebidException extends Exception {
    PrebidException(final String message) { super(message); }

    PrebidException(String message, Throwable t) {
        super(message, t);
    }
}
