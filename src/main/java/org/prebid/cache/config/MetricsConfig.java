package org.prebid.cache.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    @ConditionalOnProperty(name = "management.graphite.metrics.export.enabled", havingValue = "true")
    MeterRegistryCustomizer<MeterRegistry> graphiteMeterCustomizer() {
        return registry -> registry.config().commonTags("app", "pbc");
    }
}
