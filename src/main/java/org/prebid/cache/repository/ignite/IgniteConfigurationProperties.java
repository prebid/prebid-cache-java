package org.prebid.cache.repository.ignite;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.ignite")
public class IgniteConfigurationProperties {

    private String host;

    private Integer port;

    private String cacheName;

    private Boolean secure;
}
