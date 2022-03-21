package org.prebid.cache.functional.testcontainers.container

import org.prebid.cache.functional.testcontainers.ContainerDependencies
import org.testcontainers.containers.GenericContainer

class PrebidCacheContainer(imageName: String, config: Map<String, String>) :
        GenericContainer<PrebidCacheContainer>(imageName) {

    companion object {
        private const val PORT = 8080
        private const val DEBUG_PORT = 8000
        private const val FIXED_EXPOSED_APPLICATION_PORT = 49100
        private const val FIXED_EXPOSED_DEBUG_PORT = 49101

        private val USE_FIXED_PORTS = (System.getProperty("useFixedContainerPorts") ?: "false").toBoolean()
    }

    init {
        withNetwork(ContainerDependencies.network)
        withExposedPorts(PORT, DEBUG_PORT)
        withFixedExposedPorts()
        withDebug()
        withConfig(config)
    }

    fun getHostUri(): String {
        return "http://$host:${getHostPort()}"
    }

    private fun getHostPort(): Int {
        return getMappedPort(PORT)
    }

    private fun withConfig(config: Map<String, String>): PrebidCacheContainer {
        return withEnv(normalizeProperties(config))
    }

    private fun withFixedExposedPorts() {
        if (USE_FIXED_PORTS) {
            addFixedExposedPort(FIXED_EXPOSED_APPLICATION_PORT, PORT)
            addFixedExposedPort(FIXED_EXPOSED_DEBUG_PORT, DEBUG_PORT)
        }
    }

    private fun withDebug(): PrebidCacheContainer {
        return withEnv("JAVA_TOOL_OPTIONS",
                "-agentlib:jdwp=transport=dt_socket,address=*:$DEBUG_PORT,server=y,suspend=n")
    }

    private fun normalizeProperties(config: Map<String, String>): Map<String, String> {
        return config.mapKeys { normalizeProperty(it.key) }
    }

    private fun normalizeProperty(property: String): String {
        return property.replace(".", "_")
                .replace("-", "")
    }
}
