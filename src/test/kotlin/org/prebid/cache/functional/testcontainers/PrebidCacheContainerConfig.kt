package org.prebid.cache.functional.testcontainers

import org.prebid.cache.functional.testcontainers.container.AerospikeContainer
import org.prebid.cache.functional.testcontainers.container.AerospikeContainer.Companion.NAMESPACE
import org.prebid.cache.functional.testcontainers.container.ApacheIgniteContainer
import org.prebid.cache.functional.testcontainers.container.ApacheIgniteContainer.Companion.CACHE_NAME
import org.prebid.cache.functional.testcontainers.container.RedisContainer
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer.Companion.WEB_CACHE_PATH

class PrebidCacheContainerConfig(
    private val redisHost: String,
    private val aerospikeHost: String,
    private val apacheIgniteHost: String
) {

    fun getBaseRedisConfig(
        allowExternalUuid: Boolean,
        cacheWriteSecured: Boolean = false,
        apiKey: String? = null
    ): Map<String, String> =
        getBaseConfig(allowExternalUuid, cacheWriteSecured, apiKey) + getRedisConfig()

    fun getBaseAerospikeConfig(
        allowExternalUuid: Boolean,
        aerospikeNamespace: String = NAMESPACE,
        cacheWriteSecured: Boolean = false
    ): Map<String, String> =
        getBaseConfig(allowExternalUuid, cacheWriteSecured) + getAerospikeConfig(aerospikeNamespace)

    fun getBaseApacheIgniteConfig(
        allowExternalUuid: Boolean,
        ingineCacheName: String = CACHE_NAME,
        cacheWriteSecured: Boolean = false
    ): Map<String, String> =
        getBaseConfig(allowExternalUuid, cacheWriteSecured) + getApacheIgniteConfig(ingineCacheName)

    fun getBaseModuleStorageConfig(applicationName: String, apiKey: String): Map<String, String> =
        getBaseConfig(allowExternalUuid = true, apiKey = apiKey) +
                getModuleStorageRedisConfig(applicationName) + getRedisConfig()

    fun getCacheExpiryConfig(minExpiry: String = "15", maxExpiry: String = "28800"): Map<String, String> =
        mapOf(
            "cache.min.expiry" to minExpiry,
            "cache.max.expiry" to maxExpiry,
            "cache.expiry.sec" to "500"
        )

    fun getCacheTimeoutConfig(timeoutMs: String): Map<String, String> =
        mapOf("cache.timeout.ms" to timeoutMs)

    fun getAerospikePreventUuidDuplicationConfig(preventUuidDuplication: Boolean): Map<String, String> =
        mapOf("spring.aerospike.prevent-u-u-i-d-duplication" to preventUuidDuplication.toString())

    fun getSecondaryCacheConfig(secondaryCacheUri: String): Map<String, String> =
        mapOf(
            "cache.secondary_cache_path" to WEB_CACHE_PATH,
            "cache.secondary-uris" to secondaryCacheUri
        )

    fun getProxyCacheHostConfig(cacheHost: String): Map<String, String> =
        mapOf(
            "cache.host_param_protocol" to "http",
            "cache.allowed-proxy-host" to cacheHost
        )

    private fun getRedisConfig(): Map<String, String> =
        mapOf(
            "spring.redis.port" to RedisContainer.PORT.toString(),
            "spring.redis.host" to redisHost,
            "spring.redis.timeout" to "300"
        )

    private fun getApacheIgniteConfig(cacheName: String): Map<String, String> =
        mapOf(
            "spring.ignite.port" to ApacheIgniteContainer.PORT.toString(),
            "spring.ignite.host" to apacheIgniteHost,
            "spring.ignite.cache-name" to cacheName
        )

    private fun getAerospikeConfig(aerospikeNamespace: String): Map<String, String> =
        mapOf(
            "spring.aerospike.port" to AerospikeContainer.PORT.toString(),
            "spring.aerospike.host" to aerospikeHost,
            "spring.aerospike.cores" to "4",
            "spring.aerospike.password" to "",
            "spring.aerospike.first_backoff" to "300",
            "spring.aerospike.max_backoff" to "1000",
            "spring.aerospike.max_retry" to "3",
            "spring.aerospike.namespace" to aerospikeNamespace
        )

    private fun getModuleStorageRedisConfig(
        applicationName: String,
        timeoutMs: Long = 9999L,
    ): Map<String, String> =
        mapOf(
            "storage.redis.${applicationName}.port" to RedisContainer.PORT.toString(),
            "storage.redis.${applicationName}.host" to redisHost,
            "storage.redis.${applicationName}.timeout" to timeoutMs.toString(),
            "storage.default-ttl-seconds" to 1000L.toString()
        )

    private fun getBaseConfig(
        allowExternalUuid: Boolean,
        cacheWriteSecured: Boolean = false,
        apiKey: String? = null,
        endpoint: String = "/storage"
    ): Map<String, String> =
        getCachePrefixConfig() +
                getCacheExpiryConfig() +
                getAllowExternalUuidConfig(allowExternalUuid) +
                getCacheTimeoutConfig("2500") +
                getApiConfig(endpoint, apiKey, cacheWriteSecured)

    private fun getCachePrefixConfig(): Map<String, String> = mapOf("cache.prefix" to "prebid_")

    private fun getAllowExternalUuidConfig(allowExternalUuid: Boolean): Map<String, String> =
        mapOf("cache.allow-external-u-u-i-d" to allowExternalUuid.toString())

    private fun getApiConfig(
        endpoint: String,
        apiKey: String? = null,
        allowPublicWrite: Boolean? = null
    ): Map<String, String> = buildMap {
        put("api.storage-path", endpoint)
        apiKey?.let { put("api.api-key", it) }
        allowPublicWrite?.let { put("api.cache-write-secured", it.toString()) }
    }
}
