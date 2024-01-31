package org.prebid.cache.metrics;

public enum ProxyStatus {

    SUCCESS("success"), FAILURE("failure");

    public final String status;

    ProxyStatus(String status) {
        this.status = status;
    }
}
