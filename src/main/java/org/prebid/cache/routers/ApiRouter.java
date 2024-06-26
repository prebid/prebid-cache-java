package org.prebid.cache.routers;

import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.handlers.ErrorHandler;
import org.prebid.cache.handlers.cache.GetCacheHandler;
import org.prebid.cache.handlers.cache.PostCacheHandler;
import org.prebid.cache.handlers.storage.GetModuleStorageHandler;
import org.prebid.cache.handlers.storage.PostModuleStorageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@EnableWebFlux
@ComponentScan(basePackages = "org.prebid.cache")
@Slf4j
public class ApiRouter {

    @Bean
    RouterFunction<?> doRoute(final GetCacheHandler getCacheHandler,
                              final PostCacheHandler postCacheHandler,
                              final GetModuleStorageHandler getModuleStorageHandler,
                              final PostModuleStorageHandler postModuleStorageHandler,
                              final ErrorHandler errorHandler,
                              final ApiConfig apiConfig) {

        return route(
                POST(apiConfig.getCachePath())
                        .and(accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8, MediaType.TEXT_PLAIN)),
                postCacheHandler::save)
                .andRoute(
                        GET(apiConfig.getCachePath())
                                .and(accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8)),
                        getCacheHandler::fetch)
                .andRoute(
                        GET(apiConfig.getCachePath()).and(accept(MediaType.APPLICATION_XML)),
                        getCacheHandler::fetch)
                .andRoute(
                        POST(apiConfig.getModuleStoragePath()).and(accept(MediaType.APPLICATION_JSON)),
                        postModuleStorageHandler::save)
                .andRoute(GET(apiConfig.getModuleStoragePath()), getModuleStorageHandler::fetch)
                .andOther(route(RequestPredicates.all(), errorHandler::invalidRequest));
    }
}
