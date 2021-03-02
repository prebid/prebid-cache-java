package org.prebid.cache.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "metrics.graphite")
@ConditionalOnProperty(prefix = "metrics.graphite", name = "enabled", havingValue = "true")
@Validated
public class GraphiteConfig {
    @NotBlank
    private String host;
    private int port;
    @NotBlank
    private String prefix;
    private boolean enabled;

    @NotNull
    @Min(1)
    private Integer interval;
}
