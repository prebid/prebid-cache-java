package org.prebid.cache.repository.module.storage;

import lombok.Data;
import org.prebid.cache.repository.aerospike.AerospikeConfigurationProperties;
import org.prebid.cache.repository.redis.RedisConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class ModuleCompositeConfigurationProperties {

    private Map<String, RedisConfigurationProperties> redis;

    private Map<String, AerospikeConfigurationProperties> aerospike;
}
