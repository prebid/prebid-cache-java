package org.prebid.cache.functional.service

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.HttpResponseValidator
import io.ktor.client.features.ServerResponseException
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.host
import io.ktor.client.request.parameter
import io.ktor.client.request.port
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpStatusCode.Companion.OK
import org.prebid.cache.functional.model.request.RequestObject
import org.prebid.cache.functional.model.response.ResponseObject

class PrebidCacheApi(prebidCacheHost: String, prebidCachePort: Int) {

    private val client = HttpClient(Apache) {
        defaultRequest {
            host = prebidCacheHost
            port = prebidCachePort
            header(ContentType, Json)
        }
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        HttpResponseValidator {
            handleResponseException { exception ->
                val clientException = exception as? ClientRequestException ?: return@handleResponseException
                checkResponseStatusCode(clientException.response)
            }
            handleResponseException { exception ->
                val serverException = exception as? ServerResponseException ?: return@handleResponseException
                checkResponseStatusCode(serverException.response)
            }
        }
    }

    suspend fun getCache(uuid: String?): HttpResponse =
        client.get(CACHE_ENDPOINT) {
            if (uuid != null) parameter(UUID_QUERY_PARAMETER, uuid)
        }

    suspend fun postCache(requestObject: RequestObject, secondaryCache: String? = null): ResponseObject =
        client.post(CACHE_ENDPOINT) {
            if (secondaryCache != null) parameter(SECONDARY_CACHE_QUERY_PARAMETER, secondaryCache)
            body = requestObject
        }

    private suspend fun checkResponseStatusCode(response: HttpResponse) {
        val statusCode = response.status.value
        if (statusCode != OK.value) throw ApiException(statusCode, response.receive())
    }

    companion object {
        private const val CACHE_ENDPOINT = "/cache"
        private const val UUID_QUERY_PARAMETER = "uuid"
        private const val SECONDARY_CACHE_QUERY_PARAMETER = "secondaryCache"
    }
}
