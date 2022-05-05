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
import io.ktor.serialization.jackson.jackson
import org.prebid.cache.functional.model.request.RequestObject
import org.prebid.cache.functional.model.response.ResponseObject

class PrebidCacheApi(prebidCacheHost: String, prebidCachePort: Int) {

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

    suspend fun getCache(uuid: String?, proxyCacheHost: String? = null): HttpResponse =
        client.get(CACHE_ENDPOINT) {
            if (uuid != null) parameter(UUID_QUERY_PARAMETER, uuid)
            if (proxyCacheHost != null) parameter(PROXY_CACHE_HOST_QUERY_PARAMETER, proxyCacheHost)
        }

    suspend fun postCache(requestObject: RequestObject, secondaryCache: String? = null): ResponseObject =
        client.post(CACHE_ENDPOINT) {
            if (secondaryCache != null) parameter(SECONDARY_CACHE_QUERY_PARAMETER, secondaryCache)
            setBody(requestObject)
        }.body()

    companion object {
        private const val CACHE_ENDPOINT = "/cache"
        private const val UUID_QUERY_PARAMETER = "uuid"
        private const val PROXY_CACHE_HOST_QUERY_PARAMETER = "ch"
        private const val SECONDARY_CACHE_QUERY_PARAMETER = "secondaryCache"
    }
}
