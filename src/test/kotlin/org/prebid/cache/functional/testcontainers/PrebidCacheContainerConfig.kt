package org.prebid.cache.functional.testcontainers

import org.prebid.cache.functional.testcontainers.container.AerospikeContainer
import org.prebid.cache.functional.testcontainers.container.AerospikeContainer.Companion.NAMESPACE
import org.prebid.cache.functional.testcontainers.container.RedisContainer
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer.Companion.WEB_CACHE_PATH

class PrebidCacheContainerConfig(private val redisHost: String, private val aerospikeHost: String) {

    fun getBaseRedisConfig(allowExternalUuid: String): Map<String, String> =
        getBaseConfig(allowExternalUuid) + getRedisConfig()

    fun getBaseAerospikeConfig(allowExternalUuid: String, aerospikeNamespace: String = NAMESPACE): Map<String, String> =
        getBaseConfig(allowExternalUuid) + getAerospikeConfig(aerospikeNamespace)

    fun getBaseModuleStorageConfig(applicationName: String, apiKey: String): Map<String, String> =
        getBaseConfig("true") + getModuleStorageRedisConfig(apiKey, applicationName) + getRedisConfig()

    fun getCacheExpiryConfig(minExpiry: String = "15", maxExpiry: String = "28800"): Map<String, String> =
        mapOf(
            "cache.min.expiry" to minExpiry,
            "cache.max.expiry" to maxExpiry,
            "cache.expiry.sec" to "500"
        )

    fun getCacheTimeoutConfig(timeoutMs: String): Map<String, String> =
        mapOf("cache.timeout.ms" to timeoutMs)

    fun getAerospikePreventUuidDuplicationConfig(preventUuidDuplication: String): Map<String, String> =
        mapOf("spring.aerospike.prevent-u-u-i-d-duplication" to preventUuidDuplication)

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
        apiKey: String,
        applicationName: String,
        timeoutMs: Long = 9999L
    ): Map<String, String> =
        mapOf(
            "api.api-key" to apiKey,
            "storage.redis.${applicationName}.port" to RedisContainer.PORT.toString(),
            "storage.redis.${applicationName}.host" to redisHost,
            "storage.redis.${applicationName}.timeout" to timeoutMs.toString(),
            "storage.default-ttl-seconds" to 1000L.toString()
        )

    private fun getBaseConfig(allowExternalUuid: String): Map<String, String> =
        getCachePrefixConfig() + getCacheExpiryConfig() + getAllowExternalUuidConfig(allowExternalUuid) +
                getCacheTimeoutConfig("2500")

    private fun getCachePrefixConfig(): Map<String, String> = mapOf("cache.prefix" to "prebid_")

    private fun getAllowExternalUuidConfig(allowExternalUuid: String): Map<String, String> =
        mapOf("cache.allow-external-u-u-i-d" to allowExternalUuid)
}
