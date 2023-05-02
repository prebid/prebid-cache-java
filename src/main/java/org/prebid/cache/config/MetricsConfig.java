package org.prebid.cache.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.NamingConvention;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> identityNamingConventionMeterCustomizer() {
        // To preserve metric names as they defined in code
        return registry -> registry.config().namingConvention(NamingConvention.identity);
    }
}
