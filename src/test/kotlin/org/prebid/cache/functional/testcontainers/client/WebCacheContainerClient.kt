package org.prebid.cache.functional.testcontainers.client

import org.mockserver.client.MockServerClient
import org.mockserver.mock.Expectation
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.JsonPathBody.jsonPath
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer.Companion.WEB_CACHE_PATH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.OK

class WebCacheContainerClient(mockServerHost: String, mockServerPort: Int) {

    private val mockServerClient: MockServerClient

    init {
        mockServerClient = MockServerClient(mockServerHost, mockServerPort)
        initResponse()
    }

    fun getRecordedRequests(uuidKey: String): Array<out HttpRequest>? =
        mockServerClient.retrieveRecordedRequests(getSecondaryCacheRequest(uuidKey))

    private fun getSecondaryCacheRequest(): HttpRequest =
        request().withMethod(POST.name)
            .withPath("/$WEB_CACHE_PATH")

    private fun getSecondaryCacheRequest(uuidKey: String): HttpRequest =
        request().withMethod(POST.name)
            .withPath("/$WEB_CACHE_PATH")
            .withBody(jsonPath("\$.puts[?(@.key == '$uuidKey')]"))

    private fun initResponse(): Array<out Expectation>? =
        mockServerClient.`when`(getSecondaryCacheRequest())
            .respond(response().withStatusCode(OK.value()))
}
