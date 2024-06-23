package org.prebid.cache.repository.redis.module.storage;

import lombok.Data;
import org.prebid.cache.repository.redis.RedisConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "module.storage")
public class ModuleCompositeRedisConfigurationProperties {

    private Map<String, RedisConfigurationProperties> redis;
}
