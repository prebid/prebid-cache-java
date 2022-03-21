package org.prebid.cache.functional.service

import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.http.ContentType.JSON
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.prebid.cache.functional.model.request.RequestObject
import org.prebid.cache.functional.model.response.ResponseObject

class PrebidCacheApi(prebidCacheUrl: String) {

    companion object {
        private const val CACHE_ENDPOINT = "/cache"
        private const val UUID_QUERY_PARAMETER = "uuid"
        private const val SECONDARY_CACHE_QUERY_PARAMETER = "secondaryCache"
    }

    private val requestSpecification: RequestSpecification

    init {
        requestSpecification = buildRequestSpecification(prebidCacheUrl)
    }

    fun getCache(uuid: String?): Response {
        val request = given(requestSpecification)
        if (uuid != null) request.queryParam(UUID_QUERY_PARAMETER, uuid)
        val response: Response = request.get(CACHE_ENDPOINT)

        checkResponseStatusCode(response)
        return response
    }

    fun postCache(requestObject: RequestObject, secondaryCache: String? = null): ResponseObject {
        val request = given(requestSpecification).body(requestObject)
        if (secondaryCache != null) request.queryParam(SECONDARY_CACHE_QUERY_PARAMETER, secondaryCache)
        val response: Response = request.post(CACHE_ENDPOINT)

        checkResponseStatusCode(response)
        return response.`as`(ResponseObject::class.java)
    }

    private fun checkResponseStatusCode(response: Response) {
        val statusCode = response.statusCode
        if (statusCode != 200) throw ApiException(statusCode, response.body.asString())
    }

    private fun buildRequestSpecification(prebidCacheUrl: String): RequestSpecification {
        return RequestSpecBuilder().setBaseUri(prebidCacheUrl)
                .setContentType(JSON)
                .build()
    }
}
