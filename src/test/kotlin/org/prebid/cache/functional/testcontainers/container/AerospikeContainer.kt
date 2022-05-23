package org.prebid.cache.functional.testcontainers.container

import org.testcontainers.containers.GenericContainer

class AerospikeContainer(imageName: String) : GenericContainer<AerospikeContainer>(imageName) {

    init {
        withNamespace()
    }

    fun getContainerHost(): String = networkAliases.first()

    private fun withNamespace(): AerospikeContainer = withEnv(mapOf("NAMESPACE" to NAMESPACE))

    companion object {
        const val PORT = 3000
        const val NAMESPACE = "prebid_cache"
    }
}
