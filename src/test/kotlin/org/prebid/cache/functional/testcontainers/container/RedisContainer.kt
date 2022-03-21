package org.prebid.cache.functional.testcontainers.container

import org.testcontainers.containers.GenericContainer

class RedisContainer(imageName: String) : GenericContainer<RedisContainer>(imageName) {

    companion object {
        const val PORT = 6379
    }

    fun getContainerHost(): String {
        return networkAliases.first()
    }
}
