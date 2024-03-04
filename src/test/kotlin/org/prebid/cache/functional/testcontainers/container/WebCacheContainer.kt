package org.prebid.cache.functional.testcontainers.container

import org.testcontainers.containers.MockServerContainer
import org.testcontainers.utility.DockerImageName

class WebCacheContainer(dockerImageName: DockerImageName) : MockServerContainer(dockerImageName) {

    fun getContainerHost(): String = networkAliases.first()

    companion object {
        const val PORT = MockServerContainer.PORT
        const val WEB_CACHE_PATH = "cache"
    }
}
