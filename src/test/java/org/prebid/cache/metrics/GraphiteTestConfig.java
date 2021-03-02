package org.prebid.cache.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GraphiteTestConfig {

    @Bean
    @Primary
    public GraphiteConfig createGraphiteConfig() {
        return new GraphiteConfig("127.0.0.1", 2003, "test", false, 60);
    }
}
