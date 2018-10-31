package org.prebid.cache.repository.redis;

import io.lettuce.core.RedisURI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConditionalOnProperty(prefix = "spring.redis", name = {"endpoint"}, havingValue = "true")
@ConfigurationProperties(prefix = "spring.redis")
public class RedisPropertyConfiguration {
    private String host;
    private Integer port;
    private String password;

    RedisURI createRedisURI() {
        if (password == null) {
            return RedisURI.Builder.redis(host, port).build();
        } else {
            return RedisURI.Builder.redis(host, port).withPassword(password).build();
        }
    }
}
