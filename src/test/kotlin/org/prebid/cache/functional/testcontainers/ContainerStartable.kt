package org.prebid.cache.functional.testcontainers

import io.kotest.core.annotation.AutoScan
import io.kotest.core.listeners.ProjectListener

@AutoScan
object ContainerStartable : ProjectListener {

    override suspend fun beforeProject() = ContainerDependencies.startCacheServerContainers()

    override suspend fun afterProject() = ContainerDependencies.stopCacheServerContainers()
}
