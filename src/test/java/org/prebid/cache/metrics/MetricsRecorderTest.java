package org.prebid.cache.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.MockClock;

public class MetricsRecorderTest {

    @Bean
    @Primary
    public MeterRegistry createMeterRegistry() {
        return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
    }
}
