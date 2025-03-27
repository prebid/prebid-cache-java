package org.prebid.cache.functional

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.prebid.cache.functional.mapper.objectMapper
import org.prebid.cache.functional.model.request.RequestObject
import org.prebid.cache.functional.model.response.ResponseObject
import org.prebid.cache.functional.service.PrebidCacheApi
import org.prebid.cache.functional.testcontainers.ContainerDependencies
import org.prebid.cache.functional.testcontainers.client.WebCacheContainerClient
import org.prebid.cache.functional.testcontainers.container.WebCacheContainer
import org.prebid.cache.functional.util.getRandomUuid

class SecondaryCacheSpec : ShouldSpec({

    lateinit var webCacheContainerClient: WebCacheContainerClient
    lateinit var webCacheContainerUri: String
    lateinit var specPrebidCacheConfig: Map<String, String>
    lateinit var prebidCacheApi: PrebidCacheApi

    beforeSpec {
        // given: Mock web cache container is started
        ContainerDependencies.webCacheContainer.start()

        // and: Mock web cache client is initialized
        webCacheContainerClient = WebCacheContainerClient(
            ContainerDependencies.webCacheContainer.host,
            ContainerDependencies.webCacheContainer.serverPort
        ).apply { initSecondaryCacheResponse() }
        webCacheContainerUri =
            "http://${ContainerDependencies.webCacheContainer.getContainerHost()}:${WebCacheContainer.PORT}"

        // and: Prebid Cache with allow_external_UUID=true and configured secondary cache is started
        specPrebidCacheConfig = BaseSpec.prebidCacheConfig.getBaseAerospikeConfig(true) +
                BaseSpec.prebidCacheConfig.getSecondaryCacheConfig(webCacheContainerUri)
        prebidCacheApi = BaseSpec.getPrebidCacheApi(specPrebidCacheConfig)
    }

    afterSpec {
        // cleanup: Mock web cache container is stopped
        ContainerDependencies.webCacheContainer.stop()

        // and: Prebid Cache container is stopped
        ContainerDependencies.prebidCacheContainerPool.stopPrebidCacheContainer(specPrebidCacheConfig)
    }

    should("send a request to secondary cache when secondary cache is configured and secondaryCache query parameter is given on request") {
        // given: Request object with set payload UUID key
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply { puts[0].key = getRandomUuid() }

        // when: POST cache endpoint is called
        val responseObject: ResponseObject = prebidCacheApi.postCache(requestObject, "no")

        // then: UUID from request is returned
        responseObject.responses.size shouldBe 1
        responseObject.responses[0].uuid shouldBe requestObject.puts[0].key

        // and: Request to secondary cache was sent
        val secondaryCacheRecordedRequests =
            webCacheContainerClient.getSecondaryCacheRecordedRequests(requestObject.puts[0].key!!)
        secondaryCacheRecordedRequests?.size shouldBe 1

        // and: Request contained secondaryCache=yes query parameter
        secondaryCacheRecordedRequests!!.first().queryStringParameters
            ?.containsEntry("secondaryCache", "yes") shouldBe true

        // and: Secondary cache request body matched to the Prebid Cache request object
        val secondaryCacheRequest =
            objectMapper.readValue(secondaryCacheRecordedRequests.first().bodyAsString, RequestObject::class.java)
        secondaryCacheRequest shouldBe requestObject
    }

    should("set cache expiry equals to request 'ttlseconds' when ttlseconds parameter is given") {
        // given: Request object with set 'ttlseconds' parameter
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply {
            puts[0].key = getRandomUuid()
            puts[0].ttlseconds = 400
            puts[0].expiry = 300
        }

        // when: POST cache endpoint is called
        val responseObject: ResponseObject = prebidCacheApi.postCache(requestObject, "no")

        // then: UUID from request is returned
        responseObject.responses.size shouldBe 1
        responseObject.responses[0].uuid shouldBe requestObject.puts[0].key

        // and: Request to secondary cache was sent
        val secondaryCacheRecordedRequests =
            webCacheContainerClient.getSecondaryCacheRecordedRequests(requestObject.puts[0].key!!)
        secondaryCacheRecordedRequests?.size shouldBe 1

        // and: Secondary cache request 'expiry' parameter matches to the PBC request 'ttlseconds' parameter
        val secondaryCacheRequest =
            objectMapper.readValue(secondaryCacheRecordedRequests!!.first().bodyAsString, RequestObject::class.java)
        secondaryCacheRequest.puts.size shouldBe 1
        secondaryCacheRequest.puts[0].expiry shouldBe requestObject.puts[0].ttlseconds
    }

    should("set cache expiry from 'cache.expiry.sec' configuration property when request 'ttlseconds' and 'expiry' are absent'") {
        // given: Request object with absent 'ttlseconds' and 'expiry'
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply {
            puts[0].key = getRandomUuid()
            puts[0].ttlseconds = null
            puts[0].expiry = null
        }

        // when: POST cache endpoint is called
        val responseObject: ResponseObject = prebidCacheApi.postCache(requestObject, "no")

        // then: UUID from request is returned
        responseObject.responses.size shouldBe 1
        responseObject.responses[0].uuid shouldBe requestObject.puts[0].key

        // and: Request to secondary cache was sent
        val secondaryCacheRecordedRequests =
            webCacheContainerClient.getSecondaryCacheRecordedRequests(requestObject.puts[0].key!!)
        secondaryCacheRecordedRequests?.size shouldBe 1

        // and: Secondary cache request 'expiry' parameter matches to the Prebid Cache 'cache.expiry.sec' config property
        val secondaryCacheRequest =
            objectMapper.readValue(secondaryCacheRecordedRequests!!.first().bodyAsString, RequestObject::class.java)
        secondaryCacheRequest.puts.size shouldBe 1
        secondaryCacheRequest.puts[0].expiry shouldBe specPrebidCacheConfig["cache.expiry.sec"]?.toLong()
    }

    should("set cache expiry from 'cache.max.expiry' configuration property when request expiry > config max expiry") {
        // given: Prebid Cache configuration 'cache.max.expiry' property value
        val configCacheMaxExpiry = specPrebidCacheConfig["cache.max.expiry"]?.toLong()

        // and: Request object with set 'expiry' higher than configuration 'cache.max.expiry'
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply {
            puts[0].key = getRandomUuid()
            puts[0].ttlseconds = null
            puts[0].expiry = configCacheMaxExpiry!! + 1
        }

        // when: POST cache endpoint is called
        val responseObject: ResponseObject = prebidCacheApi.postCache(requestObject, "no")

        // then: UUID from request is returned
        responseObject.responses.size shouldBe 1
        responseObject.responses[0].uuid shouldBe requestObject.puts[0].key

        // and: Request to secondary cache was sent
        val secondaryCacheRecordedRequests =
            webCacheContainerClient.getSecondaryCacheRecordedRequests(requestObject.puts[0].key!!)
        secondaryCacheRecordedRequests?.size shouldBe 1

        // and: Secondary cache request 'expiry' parameter matches to the Prebid Cache 'cache.max.expiry' config property
        val secondaryCacheRequest =
            objectMapper.readValue(secondaryCacheRecordedRequests!!.first().bodyAsString, RequestObject::class.java)
        secondaryCacheRequest.puts.size shouldBe 1
        secondaryCacheRequest.puts[0].expiry shouldBe configCacheMaxExpiry
    }

    should("set cache expiry from 'cache.min.expiry' configuration property when request expiry < config min expiry") {
        // given: Prebid Cache configuration 'cache.min.expiry' property value
        val configCacheMinExpiry = specPrebidCacheConfig["cache.min.expiry"]?.toLong()

        // and: Request object with set 'expiry' lower than configuration 'cache.min.expiry'
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply {
            puts[0].key = getRandomUuid()
            puts[0].ttlseconds = null
            puts[0].expiry = configCacheMinExpiry!! - 1
        }

        // when: POST cache endpoint is called
        val responseObject: ResponseObject = prebidCacheApi.postCache(requestObject, "no")

        // then: UUID from request is returned
        responseObject.responses.size shouldBe 1
        responseObject.responses[0].uuid shouldBe requestObject.puts[0].key

        // and: Request to secondary cache was sent
        val secondaryCacheRecordedRequests =
            webCacheContainerClient.getSecondaryCacheRecordedRequests(requestObject.puts[0].key!!)
        secondaryCacheRecordedRequests?.size shouldBe 1

        // and: Secondary cache request 'expiry' parameter matches to the Prebid Cache 'cache.min.expiry' config property
        val secondaryCacheRequest =
            objectMapper.readValue(secondaryCacheRecordedRequests!!.first().bodyAsString, RequestObject::class.java)
        secondaryCacheRequest.puts.size shouldBe 1
        secondaryCacheRequest.puts[0].expiry shouldBe configCacheMinExpiry
    }
})
