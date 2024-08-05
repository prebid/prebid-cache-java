package org.prebid.cache.functional.testcontainers.container

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer

class ApacheIgniteContainer(imageName: String) : GenericContainer<ApacheIgniteContainer>(imageName) {

    fun getContainerHost(): String = networkAliases.first()

    init {
        withClasspathResourceMapping(
            "functional/$CONFIG_FILE_NAME",
            "/$CONFIG_FILE_NAME",
            BindMode.READ_ONLY
        )
        withCommand("/opt/ignite/apache-ignite/bin/ignite.sh", "/$CONFIG_FILE_NAME")

    }

    companion object {
        const val PORT = 10800
        const val CACHE_NAME = "prebid_cache_ignite"
        const val CONFIG_FILE_NAME = "ignite-config.xml"
    }
}
