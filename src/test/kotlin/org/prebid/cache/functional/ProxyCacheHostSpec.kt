package org.prebid.cache.functional

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import org.mockserver.model.MediaType.APPLICATION_JSON_UTF_8
import org.mockserver.model.MediaType.APPLICATION_XML
import org.prebid.cache.functional.mapper.objectMapper
import org.prebid.cache.functional.model.request.MediaType.JSON
import org.prebid.cache.functional.model.request.MediaType.XML
import org.prebid.cache.functional.model.request.TransferValue
import org.prebid.cache.functional.service.ApiException
import org.prebid.cache.functional.service.PrebidCacheApi
import org.prebid.cache.functional.testcontainers.ContainerDependencies
import org.prebid.cache.functional.testcontainers.client.WebCacheContainerClient
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer
import org.prebid.cache.functional.util.getRandomString
import org.prebid.cache.functional.util.getRandomUuid
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK

class ProxyCacheHostSpec : ShouldSpec({

    lateinit var webCacheContainerClient: WebCacheContainerClient
    lateinit var proxyCacheHost: String
    lateinit var specPrebidCacheConfig: Map<String, String>
    lateinit var prebidCacheApi: PrebidCacheApi

    beforeSpec {
        // given: Mock web cache container is started
        ContainerDependencies.webCacheContainer.start()

        // and: Mock web cache client is initialized
        webCacheContainerClient = WebCacheContainerClient(
            ContainerDependencies.webCacheContainer.host,
            ContainerDependencies.webCacheContainer.serverPort
        )
        // and: Prebid Cache with allow_external_UUID=true and configured proxy cache host is started
        proxyCacheHost =
            "${ContainerDependencies.webCacheContainer.getContainerHost()}:${WebCacheContainer.PORT}"
        specPrebidCacheConfig = BaseSpec.prebidCacheConfig.getBaseRedisConfig(true) +
                BaseSpec.prebidCacheConfig.getProxyCacheHostConfig(proxyCacheHost)
        prebidCacheApi = BaseSpec.getPrebidCacheApi(specPrebidCacheConfig)
    }

    afterSpec {
        // cleanup: Mock web cache container is stopped
        ContainerDependencies.webCacheContainer.stop()

        // and: Prebid Cache container is stopped
        ContainerDependencies.prebidCacheContainerPool.stopPrebidCacheContainer(specPrebidCacheConfig)
    }

    should("throw an exception when proxy cache host doesn't exist") {
        // given: Prebid cache config with set up not existing proxy cache host
        val cacheHost = getRandomString()
        val config = BaseSpec.prebidCacheConfig.getBaseRedisConfig(true) +
                BaseSpec.prebidCacheConfig.getProxyCacheHostConfig(cacheHost)

        // when: GET cache endpoint is called with provided cache host
        val exception =
            shouldThrowExactly<ApiException> { BaseSpec.getPrebidCacheApi(config).getCache(getRandomUuid(), cacheHost) }

        // then: Internal Server Error exception is thrown
        assertSoftly {
            exception.statusCode shouldBe INTERNAL_SERVER_ERROR.value()
            exception.responseBody shouldContain "\"message\":\"Failed to resolve '$cacheHost'"
        }
    }

    should("throw an exception when proxy cache host returns not OK status code") {
        forAll(
            row(NOT_FOUND),
            row(INTERNAL_SERVER_ERROR)
        ) { httpStatus ->
            // given: Proxy cache host response is set up to return bad status code and error message
            webCacheContainerClient.setProxyCacheHostResponse(httpStatus, httpStatus.name)

            // when: GET cache endpoint is called with provided proxy cache host
            val exception =
                shouldThrowExactly<ApiException> { prebidCacheApi.getCache(getRandomUuid(), proxyCacheHost) }

            // then: Exception thrown by proxy cache host is returned by PBC
            assertSoftly {
                exception.statusCode shouldBe httpStatus.value()
                exception.responseBody shouldContain httpStatus.name
            }
        }
    }

    should("send a request to proxy cache host when proxy cache host parameter is given on request") {
        // given: Proxy cache host response is set up
        webCacheContainerClient.setProxyCacheHostResponse(OK, getRandomString())

        // and: Initial proxy cache host request count is taken
        val initialProxyCacheHostRequestCount = webCacheContainerClient.getProxyCacheHostRecordedRequestCount()

        // when: GET cache endpoint is called with provided proxy cache host
        val requestUuid = getRandomUuid()
        prebidCacheApi.getCache(requestUuid, proxyCacheHost)

        // then: PBC called proxy cache host
        webCacheContainerClient.getProxyCacheHostRecordedRequestCount() shouldBe initialProxyCacheHostRequestCount + 1

        val proxyCacheHostRequest = webCacheContainerClient.getProxyCacheHostRecordedRequests()!!.last()
        proxyCacheHostRequest.queryStringParameters?.containsEntry("uuid", requestUuid) shouldBe true
    }

    should("return a response body as a plain String requested from proxy cache host") {
        // given: Proxy cache host response is set up to return a plain String response body
        val cacheHostResponseBody = getRandomString()
        webCacheContainerClient.setProxyCacheHostResponse(OK, cacheHostResponseBody)

        // when: GET cache endpoint is called with provided proxy cache host
        val response = prebidCacheApi.getCache(getRandomUuid(), proxyCacheHost)

        // then: PBC response body should be equal to proxy cache host response body
        response.bodyAsText() shouldBe cacheHostResponseBody
    }

    should("return a response body as a JSON or XML object requested from proxy cache host") {
        forAll(
            row(TransferValue.getDefaultJsonValue(), APPLICATION_JSON_UTF_8, JSON),
            row(TransferValue.getDefaultXmlValue(), APPLICATION_XML, XML)
        ) { proxyCacheResponseBody, proxyCacheResponseMediaType, prebidCacheResponseMediaType ->
            // given: Proxy cache host response is set up to return a JSON response body
            webCacheContainerClient.setProxyCacheHostResponse(
                OK,
                objectMapper.writeValueAsString(proxyCacheResponseBody),
                proxyCacheResponseMediaType
            )

            // when: GET cache endpoint is called with provided proxy cache host
            val response = prebidCacheApi.getCache(getRandomUuid(), proxyCacheHost)

            // then: PBC response body should be equal to proxy cache host response body
            response.contentType()?.contentType shouldBe "application"
            response.contentType()?.contentSubtype shouldBe prebidCacheResponseMediaType.getValue()

            // and: transfer value is returned
            val responseTransferValue = objectMapper.readValue(response.bodyAsText(), TransferValue::class.java)

            assertSoftly {
                responseTransferValue.adm shouldBe proxyCacheResponseBody.adm
                responseTransferValue.width shouldBe proxyCacheResponseBody.width
                responseTransferValue.height shouldBe proxyCacheResponseBody.height
            }
        }
    }
})
