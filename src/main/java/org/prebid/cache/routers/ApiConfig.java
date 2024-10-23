package org.prebid.cache.routers;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@Validated
@ConfigurationProperties(prefix = "api")
public class ApiConfig {

    @NotEmpty
    private String cachePath;

    private boolean cacheWriteSecured;

    @NotEmpty
    private String storagePath;

    @NotEmpty
    private String apiKey;

    // workaround for supporting transition period of deprecated path property
    public void setPath(String path) {
        cachePath = path;
    }
}
