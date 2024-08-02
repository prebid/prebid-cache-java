package org.prebid.cache.repository.ignite;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientConnectionException;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.SslMode;
import org.apache.ignite.configuration.ClientConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

@Configuration
@EnableConfigurationProperties(IgniteConfigurationProperties.class)
@ConditionalOnProperty(prefix = "spring.ignite", name = {"host"})
public class IgniteConfiguration {

    private static final int DEFAULT_PORT = 10800;

    @Bean
    public ClientConfiguration clientConfiguration(IgniteConfigurationProperties properties) {
        ClientConfiguration cfg = new ClientConfiguration();

        final String host = properties.getHost();
        final int port = properties.getPort() == null ? DEFAULT_PORT : properties.getPort();

        if (isCluster(host)) {
            cfg.setAddresses(extractHosts(host));
        } else {
            cfg.setAddresses(host + ":" + port);
        }

        cfg.setSslMode(BooleanUtils.isTrue(properties.getSecure()) ? SslMode.REQUIRED : SslMode.DISABLED);
        return cfg;
    }

    public static boolean isCluster(@NotNull String hostList) {
        return hostList.split(",").length > 1;
    }

    private static String[] extractHosts(@NotNull String hostList) {
        return Arrays.stream(hostList.split(","))
                .map(host -> {
                    String[] params = host.split(":");
                    String hostname = requireNonNull(params[0]);
                    int port = DEFAULT_PORT;
                    if (params.length == 2) {
                        port = Integer.parseInt(params[1]);
                    }
                    return hostname + ":" + port;
                })
                .toArray(String[]::new);
    }

    @Bean(destroyMethod = "close")
    public IgniteClient igniteClient(ClientConfiguration clientConfiguration) throws ClientConnectionException {
        return Ignition.startClient(clientConfiguration);
    }

    @Bean
    public ClientCache<String, String> igniteCache(IgniteClient igniteClient,
                                                   IgniteConfigurationProperties properties) {

        return igniteClient.cache(properties.getCacheName());
    }
}
