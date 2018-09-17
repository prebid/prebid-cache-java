package org.prebid.cache;

import org.prebid.cache.config.CorsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@SpringBootApplication
@ImportResource("classpath:spring-repository-bean.xml")
@Slf4j
public class PBCacheApplication implements WebFluxConfigurer
{
    private final CorsConfig corsConfig;

    @Autowired
    public PBCacheApplication(final CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    public static void main(String[] args) {
        SpringApplication.run(PBCacheApplication.class, args);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsConfig.isEnabled()) {
            registry.addMapping(corsConfig.getMapping())
                    .allowedOrigins(corsConfig.getAllowedOrigins())
                    .allowedMethods(corsConfig.getAllowedMethods())
                    .allowCredentials(corsConfig.isAllowCredentials());
        }
    }
}

