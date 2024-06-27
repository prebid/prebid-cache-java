package org.prebid.cache.functional.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import org.prebid.cache.functional.model.request.PayloadTransfer
import org.prebid.cache.functional.model.request.RequestObject
import org.prebid.cache.functional.model.response.ResponseObject

class PrebidCacheApi(prebidCacheHost: String, prebidCachePort: Int) {

    suspend fun getCache(uuid: String?, proxyCacheHost: String? = null): HttpResponse =
        get(CACHE_ENDPOINT, mapOf(UUID_QUERY_PARAMETER to uuid, PROXY_CACHE_HOST_QUERY_PARAMETER to proxyCacheHost))

    suspend fun postCache(requestObject: RequestObject, secondaryCache: String? = null): ResponseObject =
        post(CACHE_ENDPOINT, requestObject, mapOf(SECONDARY_CACHE_QUERY_PARAMETER to secondaryCache)).body()

    suspend fun getModuleStorageCache(
        payloadTransferKey: String?,
        application: String?,
        apiKey: String?
    ): PayloadTransfer =
        get(
            MODULE_STORAGE_ENDPOINT,
            mapOf(KEY_PARAMETER to payloadTransferKey, APPLICATION_PARAMETER to application),
            mapOf(API_KEY_PARAMETER to apiKey)
        ).body()

    suspend fun postModuleStorageCache(requestObject: PayloadTransfer, apiKey: String? = null): Boolean =
        post(
            MODULE_STORAGE_ENDPOINT,
            requestObject,
            headers = mapOf(API_KEY_PARAMETER to apiKey)
        ).status == HttpStatusCode.NoContent

    private val client = HttpClient(Apache) {
        expectSuccess = true
        defaultRequest {
            host = prebidCacheHost
            port = prebidCachePort
            header(ContentType, Json)
        }
        install(ContentNegotiation) {
            jackson()
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                val clientException = exception as? ResponseException ?: return@handleResponseExceptionWithRequest
                val statusCode = clientException.response.status
                throw ApiException(statusCode.value, clientException.response.bodyAsText())
            }
        }
    }

    private suspend fun get(
        endpoint: String,
        parameters: Map<String, String?> = emptyMap(),
        headers: Map<String, String?> = emptyMap()
    ): HttpResponse = client.get(endpoint) {
        parameters.forEach { (key, value) ->
            value?.let { parameter(key, value) }
        }
        headers.forEach { (key, value) ->
            value?.let { header(key, value) }
        }
    }

    private suspend fun post(
        endpoint: String,
        requestObject: Any,
        parameters: Map<String, String?> = emptyMap(),
        headers: Map<String, String?> = emptyMap()
    ): HttpResponse =
        client.post(endpoint) {
            setBody(requestObject)
            parameters.forEach { (key, value) ->
                value?.let { parameter(key, value) }
            }
            headers.forEach { (key, value) ->
                value?.let { header(key, value) }
            }
        }

    companion object {
        private const val CACHE_ENDPOINT = "/cache"
        private const val UUID_QUERY_PARAMETER = "uuid"
        private const val PROXY_CACHE_HOST_QUERY_PARAMETER = "ch"
        private const val SECONDARY_CACHE_QUERY_PARAMETER = "secondaryCache"

        private const val MODULE_STORAGE_ENDPOINT = "/module-storage"
        private const val API_KEY_PARAMETER = "x-pbc-api-key"
        private const val KEY_PARAMETER = "k"
        private const val APPLICATION_PARAMETER = "a"
    }
}
