package org.prebid.cache.functional.testcontainers.container

import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Ulimit
import org.testcontainers.containers.GenericContainer

class AerospikeContainer(imageName: String) : GenericContainer<AerospikeContainer>(imageName) {

    init {
        withNamespace()
        configureUlimit()
    }

    fun getContainerHost(): String = networkAliases.first()

    private fun withNamespace(): AerospikeContainer = withEnv(mapOf("NAMESPACE" to NAMESPACE))

    private fun configureUlimit() {
        withCreateContainerCmdModifier { cmd ->
            val limit = Ulimit("nofile", 15000L, 15000L)
            val hostConfig = HostConfig().withUlimits(listOf(limit))
            cmd.withHostConfig(hostConfig)
        }
    }

    companion object {
        const val PORT = 3002
        const val NAMESPACE = "prebid_cache"
    }
}
