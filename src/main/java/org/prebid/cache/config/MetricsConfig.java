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
    MeterRegistryCustomizer<MeterRegistry> identityNamingConventionMeterCustomizer() {
        // To preserve metric names as they defined in code
        return registry -> registry.config().namingConvention(NamingConvention.identity);
    }

    @Bean
    @ConditionalOnProperty(name = "management.metrics.export.graphite.enabled", havingValue = "true")
    MeterRegistryCustomizer<MeterRegistry> graphitePrefixMeterCustomizer(
        @Value("${management.metrics.export.graphite.prefix:}") String prefix) {

        return registry -> {
            if (StringUtils.isNotEmpty(prefix)) {
                registry.config().commonTags("prefix", prefix);
            }
        };
    }
}
