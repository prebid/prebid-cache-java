package org.prebid.cache.repository.redis;

import io.lettuce.core.RedisURI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConditionalOnProperty(prefix = "spring.redis", name = {"host"})
@ConfigurationProperties(prefix = "spring.redis.sentinel")
public class RedisSentinelPropertyConfiguration {
    private String master;
    private List<String> nodes;

    RedisURI createRedisURI(RedisPropertyConfiguration redisConfig) {
        if (redisConfig.getPassword() == null) {
            return RedisURI.Builder.sentinel(redisConfig.getHost(), redisConfig.getPort(), master).build();
        } else {
            RedisURI.Builder builder = RedisURI.Builder.sentinel(redisConfig.getHost(), redisConfig.getPort(), master)
                    .withPassword(redisConfig.getPassword());
            for (String node : nodes) {
                String[] host = node.split(":");
                if (host.length == 1) {
                    builder.withSentinel(node);
                } else {
                    builder.withSentinel(host[0], Integer.parseInt(host[1]));
                }
            }
            return builder.build();
        }
    }
}
