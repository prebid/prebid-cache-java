package org.prebid.cache.routers;

import org.prebid.cache.handlers.GetCacheHandler;
import org.prebid.cache.handlers.PostCacheHandler;
import org.prebid.cache.handlers.ErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.http.MediaType.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@EnableWebFlux
@ComponentScan(basePackages = "org.prebid.cache")
@Slf4j
public class ApiRouter
{
    @Bean
    RouterFunction<?> doRoute(final GetCacheHandler getCacheHandler,
                              final PostCacheHandler postCacheHandler,
                              final ErrorHandler errorHandler,
                              final ApiConfig apiConfig) {
        return route(POST(apiConfig.getPath())
                    .and(accept(APPLICATION_JSON, APPLICATION_JSON_UTF8)), postCacheHandler::save)
                .andRoute(GET(apiConfig.getPath())
                    .and(accept(APPLICATION_JSON, APPLICATION_JSON_UTF8)), getCacheHandler::fetch)
                .andRoute(GET(apiConfig.getPath())
                    .and(accept(APPLICATION_XML)), getCacheHandler::fetch)
                .andOther(route(RequestPredicates.all(), errorHandler::invalidRequest));
    }
}