package org.prebid.cache.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@ConditionalOnProperty(name = "server.max-http-body-size-kb")
public class BodySizeConfig implements WebFluxConfigurer {

    @Value("${server.max-http-body-size-kb}")
    private int maxHttpBodySizeKb;

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(maxHttpBodySizeKb * 1024);
    }
}
