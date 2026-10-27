package org.prebid.cache.functional.testcontainers.container

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class WebCacheContainer(dockerImageName: DockerImageName) : GenericContainer<WebCacheContainer>(dockerImageName) {

    fun getContainerHost(): String = networkAliases.first()

    init {
        withExposedPorts(PORT)
    }

    val serverPort: Int
        get() = getMappedPort(PORT)

    companion object {
        const val PORT = 8080
        const val WEB_CACHE_PATH = "cache"
    }
}
