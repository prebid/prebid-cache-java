package org.prebid.cache.functional.testcontainers.client

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import java.util.UUID
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer.Companion.WEB_CACHE_PATH
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType

class WebCacheContainerClient(mockServerHost: String, mockServerPort: Int) {

    private val wireMockClient = WireMock(mockServerHost, mockServerPort)
    private var proxyCacheHostStub: StubMapping? = null

    fun getProxyCacheHostRecordedRequests(): List<LoggedRequest> =
        wireMockClient.find(getProxyCacheHostRequest())

    fun getProxyCacheHostRecordedRequestCount(): Int =
        getProxyCacheHostRecordedRequests().size

    fun setProxyCacheHostResponse(
        httpStatus: HttpStatus = OK,
        body: String = "{}",
        mediaType: MediaType = MediaType.APPLICATION_JSON
    ): StubMapping {
        proxyCacheHostStub?.let { wireMockClient.removeStubMapping(it) }

        return wireMockClient.register(
            get(urlPathEqualTo("/$WEB_CACHE_PATH"))
                .inScenario("proxy-cache-host-${UUID.randomUUID()}")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(
                    aResponse()
                        .withStatus(httpStatus.value())
                        .withBody(body)
                        .withHeader("Content-Type", mediaType.toString())
                        .withHeader("Connection", "close")
                )
                .willSetStateTo("CONSUMED")
        ).also { proxyCacheHostStub = it }
    }

    fun getSecondaryCacheRecordedRequests(uuidKey: String): List<LoggedRequest> {
        val secondaryCacheRequest = getSecondaryCacheRequest(uuidKey)
        waitUntil({ wireMockClient.find(secondaryCacheRequest).isNotEmpty() })
        return wireMockClient.find(secondaryCacheRequest)
    }

    fun initSecondaryCacheResponse(): StubMapping =
        wireMockClient.register(
            post(urlPathEqualTo("/$WEB_CACHE_PATH"))
                .willReturn(
                    aResponse()
                        .withStatus(OK.value())
                        .withHeader("Connection", "close")
                )
        )

    private fun getProxyCacheHostRequest() =
        getRequestedFor(urlPathEqualTo("/$WEB_CACHE_PATH"))

    private fun getSecondaryCacheRequest() =
        postRequestedFor(urlPathEqualTo("/$WEB_CACHE_PATH"))

    private fun getSecondaryCacheRequest(uuidKey: String) =
        getSecondaryCacheRequest()
            .withRequestBody(matchingJsonPath("$.puts[?(@.key == '$uuidKey')]"))

    private fun waitUntil(closure: () -> Boolean, timeoutMs: Long = 5000, pollInterval: Long = 100) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime <= timeoutMs) {
            if (closure()) {
                return
            }
            Thread.sleep(pollInterval)
        }
        throw IllegalStateException("Condition was not fulfilled within $timeoutMs ms.")
    }
}
