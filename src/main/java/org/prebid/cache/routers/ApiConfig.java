package org.prebid.cache.routers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig {

    private String cachePath;

    private String moduleStoragePath;

    // workaround for supporting transition period of deprecated path property
    public void setPath(String path) {
        cachePath = path;
    }
}
