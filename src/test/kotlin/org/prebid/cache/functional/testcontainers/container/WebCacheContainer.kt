package org.prebid.cache.functional.testcontainers.container

import org.testcontainers.containers.MockServerContainer

class WebCacheContainer(mockServerImageVersion: String) : MockServerContainer(mockServerImageVersion) {

    companion object {
        const val PORT = MockServerContainer.PORT
        const val WEB_CACHE_PATH = "cache"
    }

    fun getContainerHost(): String {
        return networkAliases.first()
    }
}
