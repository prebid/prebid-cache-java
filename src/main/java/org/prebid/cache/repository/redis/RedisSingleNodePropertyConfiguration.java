package org.prebid.cache.repository.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@Validated
@ConditionalOnProperty(prefix = "spring.redis.single-node", name = {"timeout"})
@ConfigurationProperties(prefix = "spring.redis.single-node")
public class RedisSingleNodePropertyConfiguration {

    @NotNull
    private int port;

    @NotNull
    private String host;

    @NotNull
    private long timeout;

    private String password;

    private RedisURI createRedisURI(String host, int port) {
        requireNonNull(host);
        final RedisURI.Builder builder = RedisURI.Builder.redis(host, port)
                .withTimeout(Duration.ofMillis(timeout));
        if (password != null) {
            builder.withPassword((CharSequence) password);
        }

        return builder.build();
    }

    @Bean(destroyMethod = "shutdown")
    RedisClient client() {
        return RedisClient.create(createRedisURI(getHost(), getPort()));
    }

    @Bean(destroyMethod = "close")
    StatefulRedisConnection<String, String> connection() {
        return client().connect();
    }

    @Bean
    RedisStringReactiveCommands<String, String> reactiveCommands() {
        return connection().reactive();
    }

}
