package org.prebid.cache.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@ConfigurationProperties(prefix = "storage")
public class StorageConfig {

    @NotNull
    @Min(0)
    Long defaultTtlSeconds;
}
