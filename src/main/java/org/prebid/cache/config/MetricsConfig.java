package org.prebid.cache.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.NamingConvention;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    @ConditionalOnProperty(name = "management.graphite.metrics.export.enabled", havingValue = "true")
    MeterRegistryCustomizer<MeterRegistry> graphiteMeterCustomizer(
            @Value("${management.graphite.metrics.export.prefix:}") String prefix) {

        return registry -> {
            if (StringUtils.isNotBlank(prefix)) {
                registry.config().commonTags("prefix", prefix);
            }
            registry.config().namingConvention(NamingConvention.identity);
        };
    }
}
