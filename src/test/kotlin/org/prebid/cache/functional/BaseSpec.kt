package org.prebid.cache.functional

import org.prebid.cache.functional.service.PrebidCacheApi
import org.prebid.cache.functional.testcontainers.ContainerDependencies
import org.prebid.cache.functional.testcontainers.PrebidCacheContainerConfig

abstract class BaseSpec {

    companion object {
        val prebidCacheConfig = PrebidCacheContainerConfig(ContainerDependencies.redisContainer.getContainerHost(),
                ContainerDependencies.aerospikeContainer.getContainerHost())

        fun getPrebidCacheApi(config: Map<String, String> = prebidCacheConfig.getBaseRedisConfig("false")): PrebidCacheApi {
            val prebidCacheContainer =
                    ContainerDependencies.prebidCacheContainerPool.getPrebidCacheContainer(config)
            return PrebidCacheApi(prebidCacheContainer.getHostUri())
        }
    }
}
