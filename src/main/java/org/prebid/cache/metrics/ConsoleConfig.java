package org.prebid.cache.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "metrics.console")
@ConditionalOnProperty(prefix = "metrics.console", name = "enabled", havingValue = "true")
@Validated
@Data
@NoArgsConstructor
public class ConsoleConfig {

    @NotNull
    @Min(1)
    private Integer interval;
}
