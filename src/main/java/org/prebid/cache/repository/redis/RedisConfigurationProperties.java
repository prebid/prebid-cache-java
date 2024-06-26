package org.prebid.cache.repository.redis;

import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
public class RedisConfigurationProperties {

    private String host;
    private long timeout;
    private String password;
    private int port;
    private Cluster cluster;

    @Data
    public static class Cluster {

        @Singular
        List<String> nodes;

        boolean enableTopologyRefresh;

        Integer topologyPeriodicRefreshPeriod;
    }
}
