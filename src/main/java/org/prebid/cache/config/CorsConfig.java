package org.prebid.cache.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsConfig {
    private boolean enabled;
    private String mapping;
    private String[] allowedOrigins;
    private String[] allowedMethods;
    private boolean allowCredentials;
}
