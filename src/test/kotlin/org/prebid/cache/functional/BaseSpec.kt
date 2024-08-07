package org.prebid.cache.functional

import org.prebid.cache.functional.service.PrebidCacheApi
import org.prebid.cache.functional.testcontainers.ContainerDependencies
import org.prebid.cache.functional.testcontainers.PrebidCacheContainerConfig

abstract class BaseSpec {

    companion object {
        val prebidCacheConfig = PrebidCacheContainerConfig(
            ContainerDependencies.redisContainer.getContainerHost(),
            ContainerDependencies.aerospikeContainer.getContainerHost(),
            ContainerDependencies.apacheIgniteContainer.getContainerHost(),
        )

        fun getPrebidCacheApi(config: Map<String, String> = prebidCacheConfig.getBaseRedisConfig("false")): PrebidCacheApi {
            return ContainerDependencies.prebidCacheContainerPool.getPrebidCacheContainer(config)
                .let { container -> PrebidCacheApi(container.host, container.getHostPort()) }
        }
    }
}
