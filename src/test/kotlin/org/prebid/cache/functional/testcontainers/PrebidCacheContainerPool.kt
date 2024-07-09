package org.prebid.cache.functional.testcontainers

import org.prebid.cache.functional.testcontainers.container.PrebidCacheContainer

class PrebidCacheContainerPool(private val containerImageName: String) {

    private val prebidCacheContainerMap: MutableMap<Map<String, String>, PrebidCacheContainer> = mutableMapOf()

    fun getPrebidCacheContainer(config: Map<String, String>): PrebidCacheContainer {
        if (prebidCacheContainerMap.size >= MAX_CONTAINER_COUNT) {
            val oldestContainerConfig =
                prebidCacheContainerMap.entries.sortedBy { it.value.containerInfo.created }[0].key
            stopPrebidCacheContainer(oldestContainerConfig)
        }

        return if (prebidCacheContainerMap.containsKey(config)) {
            prebidCacheContainerMap.getValue(config)
        } else {
            val prebidCacheContainer = PrebidCacheContainer(containerImageName, config)
            prebidCacheContainer.start()
            prebidCacheContainerMap[config] = prebidCacheContainer
            prebidCacheContainer
        }
    }

    fun stopPrebidCacheContainer(config: Map<String, String>) {
        if (prebidCacheContainerMap.containsKey(config)) prebidCacheContainerMap.getValue(config).stop()
        prebidCacheContainerMap.remove(config)
    }

    companion object {
        private val MAX_CONTAINER_COUNT: Int = System.getProperty("max.containers.count")?.toInt() ?: 4
    }
}
