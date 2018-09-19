package org.prebid.cache.exceptions;

public class RepositoryException extends PrebidException {
    public RepositoryException(final String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable t) {
        super(message, t);
    }
}
