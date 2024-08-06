package org.prebid.cache.functional.testcontainers

import org.prebid.cache.functional.testcontainers.container.AerospikeContainer
import org.prebid.cache.functional.testcontainers.container.ApacheIgniteContainer
import org.prebid.cache.functional.testcontainers.container.RedisContainer
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer
import org.testcontainers.containers.Network
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName

abstract class ContainerDependencies {

    companion object {
        private const val redisImageName = "redis:6.2.6-alpine"
        private const val aerospikeImageName = "aerospike:ce-5.7.0.11"
        private const val apacheIgniteImageName = "apacheignite/ignite:2.16.0"
        private const val prebidCacheImageName = "prebid-cache:latest"
        private const val mockServerImageVersion = "mockserver/mockserver:5.15.0"

        private val launchContainers = System.getProperty("launchContainers")?.toBoolean() ?: true

        val network: Network = Network.newNetwork()
        val redisContainer: RedisContainer = RedisContainer(redisImageName).withNetwork(network)
        val aerospikeContainer: AerospikeContainer = AerospikeContainer(aerospikeImageName).withNetwork(network)
        val apacheIgniteContainer: ApacheIgniteContainer = ApacheIgniteContainer(apacheIgniteImageName).withNetwork(network)
        val webCacheContainer: WebCacheContainer =
            WebCacheContainer(DockerImageName.parse(mockServerImageVersion)).withNetwork(network) as WebCacheContainer
        val prebidCacheContainerPool = PrebidCacheContainerPool(prebidCacheImageName)

        fun startCacheServerContainers() {
            if (launchContainers) {
                Startables.deepStart(listOf(redisContainer, aerospikeContainer, apacheIgniteContainer)).join()
            }
        }

        fun stopCacheServerContainers() {
            if (launchContainers) {
                listOf(redisContainer, aerospikeContainer, apacheIgniteContainer).parallelStream().forEach { it.stop() }
            }
        }
    }
}
