package org.prebid.cache.functional.testcontainers.container

import org.testcontainers.containers.GenericContainer

class RedisContainer(imageName: String) : GenericContainer<RedisContainer>(imageName) {

    fun getContainerHost(): String = networkAliases.first()

    companion object {
        const val PORT = 6379
    }
}
