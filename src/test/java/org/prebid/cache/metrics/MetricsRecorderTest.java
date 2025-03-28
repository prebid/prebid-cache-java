package org.prebid.cache.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class MetricsRecorderTest {

    @Bean
    @Primary
    public MeterRegistry createMeterRegistry() {
        return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
    }
}
