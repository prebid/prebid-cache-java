package org.prebid.cache.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class MetricsConfiguration {

    static final String METRIC_REGISTRY_NAME = "metric-registry";

    @Bean
    MetricRegistry metricRegistry() {
        final boolean alreadyExists = SharedMetricRegistries.names().contains(METRIC_REGISTRY_NAME);
        final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(METRIC_REGISTRY_NAME);

        if (!alreadyExists) {
            metricRegistry.register("jvm.gc", new GarbageCollectorMetricSet());
            metricRegistry.register("jvm.memory", new MemoryUsageGaugeSet());
        }
        return metricRegistry;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "metrics.graphite", name = "enabled", havingValue = "true")
    ScheduledReporter graphiteReporter(GraphiteConfig graphiteProperties, MetricRegistry metricRegistry) {
        log.info("Starting graphite metrics reporter host - [{}:{}].", graphiteProperties.getHost(),
                graphiteProperties.getPort());

        final Graphite graphite = new Graphite(graphiteProperties.getHost(), graphiteProperties.getPort());
        final ScheduledReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
                .prefixedWith(graphiteProperties.getPrefix())
                .build(graphite);

        reporter.start(graphiteProperties.getInterval(), TimeUnit.SECONDS);

        return reporter;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "metrics.console", name = "enabled", havingValue = "true")
    ScheduledReporter consoleReporter(ConsoleConfig consoleConfig, MetricRegistry metricRegistry) {
        log.info("Starting console metrics reporter");

        final ScheduledReporter reporter = ConsoleReporter.forRegistry(metricRegistry).build();
        reporter.start(consoleConfig.getInterval(), TimeUnit.SECONDS);

        return reporter;
    }
}
