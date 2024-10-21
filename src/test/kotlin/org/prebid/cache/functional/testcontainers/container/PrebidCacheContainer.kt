package org.prebid.cache.functional.testcontainers.container

import org.prebid.cache.functional.testcontainers.ContainerDependencies
import org.testcontainers.containers.GenericContainer

class PrebidCacheContainer(imageName: String, config: Map<String, String>) :
    GenericContainer<PrebidCacheContainer>(imageName) {

    init {
        withNetwork(ContainerDependencies.network)
        withExposedPorts(PORT, DEBUG_PORT, ADMIN_PORT)
        withFixedExposedPorts()
        withJavaToolOptions()
        withConfig(config)
    }

    fun getHostPort(): Int = getMappedPort(PORT)
    fun getHostAdminPort(): Int = getMappedPort(ADMIN_PORT)

    private fun withConfig(config: Map<String, String>): PrebidCacheContainer =
        withEnv(normalizeProperties(config))

    private fun withFixedExposedPorts() {
        if (USE_FIXED_PORTS) {
            addFixedExposedPort(FIXED_EXPOSED_APPLICATION_PORT, PORT)
            addFixedExposedPort(FIXED_EXPOSED_DEBUG_PORT, DEBUG_PORT)
            addFixedExposedPort(FIXED_EXPOSED_ADMIN_PORT, ADMIN_PORT)
        }
    }

    private fun withJavaToolOptions(): PrebidCacheContainer {
        val debugOptions = "-agentlib:jdwp=transport=dt_socket,address=*:$DEBUG_PORT,server=y,suspend=n"
        val opensOptions = listOf(
            "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
            "--add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED",
            "--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED",
            "--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED",
            "--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.sql/java.sql=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.time=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.management/sun.management=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED"
        ).joinToString(" ")

        return withEnv("JAVA_TOOL_OPTIONS", "$debugOptions $opensOptions")
    }

    private fun normalizeProperties(config: Map<String, String>): Map<String, String> =
        config.mapKeys { normalizeProperty(it.key) }

    private fun normalizeProperty(property: String): String =
        property.replace(".", "_")
            .replace("-", "")

    companion object {
        private const val PORT = 8080
        private const val DEBUG_PORT = 8000
        private const val ADMIN_PORT = 8081
        private const val FIXED_EXPOSED_APPLICATION_PORT = 49100
        private const val FIXED_EXPOSED_DEBUG_PORT = 49101
        private const val FIXED_EXPOSED_ADMIN_PORT = 49102

        private val USE_FIXED_PORTS = System.getProperty("useFixedContainerPorts")?.toBoolean() ?: false
    }
}
