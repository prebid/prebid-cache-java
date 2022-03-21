package org.prebid.cache.functional.testcontainers.container

import org.testcontainers.containers.GenericContainer

class AerospikeContainer(imageName: String) : GenericContainer<AerospikeContainer>(imageName) {

    companion object {
        const val PORT = 3000
        const val NAMESPACE = "prebid_cache"
    }

    init {
        withNamespace()
    }

    fun getContainerHost(): String {
        return networkAliases.first()
    }

    private fun withNamespace(): AerospikeContainer {
        return withEnv(mapOf("NAMESPACE" to NAMESPACE))
    }
}
