package org.prebid.cache.functional.testcontainers.client

import org.mockserver.client.MockServerClient
import org.mockserver.matchers.Times
import org.mockserver.mock.Expectation
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.JsonPathBody.jsonPath
import org.mockserver.model.MediaType
import org.mockserver.model.MediaType.APPLICATION_JSON_UTF_8
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer.Companion.WEB_CACHE_PATH
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK

class WebCacheContainerClient(mockServerHost: String, mockServerPort: Int) {

    private val mockServerClient: MockServerClient

    init {
        mockServerClient = MockServerClient(mockServerHost, mockServerPort)
    }

    fun getProxyCacheHostRecordedRequests(): Array<out HttpRequest>? =
        mockServerClient.retrieveRecordedRequests(getProxyCacheHostRequest())

    fun getProxyCacheHostRecordedRequestCount(): Int =
        getProxyCacheHostRecordedRequests()?.size ?: 0

    fun setProxyCacheHostResponse(
        httpStatus: HttpStatus = OK,
        body: String = "",
        mediaType: MediaType = APPLICATION_JSON_UTF_8
    ): Array<out Expectation>? =
        mockServerClient.`when`(getProxyCacheHostRequest(), Times.exactly(1))
            .respond(
                response().withStatusCode(httpStatus.value())
                    .withBody(body, mediaType)
            )

    fun getSecondaryCacheRecordedRequests(uuidKey: String): Array<out HttpRequest>? =
        mockServerClient.retrieveRecordedRequests(getSecondaryCacheRequest(uuidKey))

    fun initSecondaryCacheResponse(): Array<out Expectation>? =
        mockServerClient.`when`(getSecondaryCacheRequest())
            .respond(response().withStatusCode(OK.value()))

    private fun getProxyCacheHostRequest(): HttpRequest =
        request().withMethod(GET.name)
            .withPath("/$WEB_CACHE_PATH")

    private fun getSecondaryCacheRequest(): HttpRequest =
        request().withMethod(POST.name)
            .withPath("/$WEB_CACHE_PATH")

    private fun getSecondaryCacheRequest(uuidKey: String): HttpRequest =
        request().withMethod(POST.name)
            .withPath("/$WEB_CACHE_PATH")
            .withBody(jsonPath("\$.puts[?(@.key == '$uuidKey')]"))
}
