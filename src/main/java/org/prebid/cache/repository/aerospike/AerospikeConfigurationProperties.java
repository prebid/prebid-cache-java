package org.prebid.cache.repository.aerospike;

import com.aerospike.client.Host;
import com.aerospike.client.policy.Replica;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

@Data
public class AerospikeConfigurationProperties {

    private static final int DEFAULT_PORT = 3000;

    private String host;
    private Integer port;

    private String password;
    private Integer cores;
    private Long firstBackoff;
    private Long maxBackoff;
    private int maxRetry;
    private String namespace;
    private boolean preventUUIDDuplication;
    private int socketTimeout;
    private int totalTimeout;
    private int connectTimeout;
    private int minConnsPerNode;
    private int maxConnsPerNode = 100;
    private Replica readPolicy = Replica.SEQUENCE;

    public static Host[] extractHosts(@NotNull String hostList) {
        return Arrays.stream(hostList.split(","))
                .map(host -> {
                    String[] params = host.split(":");
                    String hostname = requireNonNull(params[0]);
                    int port = DEFAULT_PORT;
                    if (params.length == 2) {
                        port = Integer.parseInt(params[1]);
                    }
                    return new Host(hostname, port);
                })
                .toArray(Host[]::new);
    }

    public static boolean isAerospikeCluster(@NotNull String hostList) {
        return hostList.split(",").length > 1;
    }
}
