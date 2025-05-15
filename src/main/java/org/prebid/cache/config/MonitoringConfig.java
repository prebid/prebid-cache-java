package org.prebid.cache.config;

import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.CacheConfig;
import org.prebid.cache.repository.ReactiveRepository;
import org.prebid.cache.service.CacheMonitorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Random;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "cache.monitoring", name = "enabled", havingValue = "true")
public class MonitoringConfig {

    @Bean
    public CacheMonitorService storageMonitorService(ReactiveRepository<PayloadWrapper, String> repository,
                                                     MetricsRecorder metricsRecorder,
                                                     CacheConfig config) {
        return new CacheMonitorService(repository, metricsRecorder, config);
    }

    @Bean
    public Disposable monitorScheduledPoller(CacheMonitorService cacheMonitorService,
                                             @Value("${cache.monitoring.intervalSec}") int intervalSec,
                                             @Value("${cache.monitoring.maxStartDelayJitterSec}") int startJitterSec) {
        final Duration startDelay = startJitterSec > 0
                ? Duration.ofSeconds(new Random().nextInt(0, startJitterSec))
                : Duration.ZERO;
        return Flux.interval(startDelay, Duration.ofSeconds(intervalSec))
                .onBackpressureDrop()
                .concatMap(counter -> cacheMonitorService.poll())
                .onErrorContinue((throwable, o) -> log.error(
                        "Failed during cache monitor polling: " + throwable.getMessage(), throwable))
                .subscribe();
    }
}
