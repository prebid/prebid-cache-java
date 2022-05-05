package org.prebid.cache.functional.testcontainers

import org.prebid.cache.functional.testcontainers.container.AerospikeContainer
import org.prebid.cache.functional.testcontainers.container.RedisContainer
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer
import org.testcontainers.containers.Network
import org.testcontainers.lifecycle.Startables

abstract class ContainerDependencies {

    companion object {
        private const val redisImageName = "redis:6.2.6-alpine"
        private const val aerospikeImageName = "aerospike:ce-5.7.0.11"
        private const val prebidCacheImageName = "prebid-cache:latest"
        private const val mockServerImageVersion = "5.13.2"

        val network: Network = Network.newNetwork()
        val redisContainer: RedisContainer = RedisContainer(redisImageName).withNetwork(network)
        val aerospikeContainer: AerospikeContainer = AerospikeContainer(aerospikeImageName).withNetwork(network)
        val webCacheContainer: WebCacheContainer =
            WebCacheContainer(mockServerImageVersion).withNetwork(network) as WebCacheContainer
        val prebidCacheContainerPool = PrebidCacheContainerPool(prebidCacheImageName)

        fun startCacheServerContainers() {
            Startables.deepStart(listOf(redisContainer, aerospikeContainer))
                .join()
        }

        fun stopCacheServerContainers() =
            listOf(redisContainer, aerospikeContainer).parallelStream()
                .forEach { it.stop() }
    }
}
