package org.prebid.cache.functional.testcontainers.container

import org.testcontainers.containers.MockServerContainer

class WebCacheContainer(mockServerImageVersion: String) : MockServerContainer(mockServerImageVersion) {

    fun getContainerHost(): String = networkAliases.first()

    companion object {
        const val PORT = MockServerContainer.PORT
        const val WEB_CACHE_PATH = "cache"
    }
}
