package org.prebid.cache.functional.testcontainers.client

import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.JsonPathBody.jsonPath
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer.Companion.WEB_CACHE_PATH

class WebCacheContainerClient(mockServerHost: String, mockServerPort: Int) {

    private val mockServerClient: MockServerClient

    init {
        mockServerClient = MockServerClient(mockServerHost, mockServerPort)
        initResponse()
    }

    fun getRecordedRequests(uuidKey: String): Array<out HttpRequest>? {
        return mockServerClient.retrieveRecordedRequests(getSecondaryCacheRequest(uuidKey))
    }

    private fun getSecondaryCacheRequest(): HttpRequest {
        return request().withMethod("POST")
                .withPath("/$WEB_CACHE_PATH")
    }

    private fun getSecondaryCacheRequest(uuidKey: String): HttpRequest {
        return request().withMethod("POST")
                .withPath("/$WEB_CACHE_PATH")
                .withBody(jsonPath("\$.puts[?(@.key == '$uuidKey')]"))
    }

    private fun initResponse() {
        mockServerClient.`when`(getSecondaryCacheRequest())
                .respond(response().withStatusCode(200))
    }
}