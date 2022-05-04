package org.prebid.cache.functional

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.prebid.cache.functional.BaseSpec.Companion.prebidCacheConfig
import org.prebid.cache.functional.model.request.PayloadTransfer
import org.prebid.cache.functional.model.request.RequestObject
import org.prebid.cache.functional.model.request.TransferValue
import org.prebid.cache.functional.model.response.ResponseObject
import org.prebid.cache.functional.service.ApiException
import org.prebid.cache.functional.testcontainers.ContainerDependencies
import org.prebid.cache.functional.util.PrebidCacheUtil

class AerospikeCacheSpec : ShouldSpec({

    should("throw an exception when cache record is absent in Aerospike repository") {
        // given: Prebid cache config
        val config = prebidCacheConfig.getBaseAerospikeConfig("true")
        val cachePrefix = config["cache.prefix"]

        // when: GET cache endpoint with random UUID is called
        val randomUuid = PrebidCacheUtil.getRandomUuid()
        val exception = shouldThrowExactly<ApiException> { BaseSpec.getPrebidCacheApi(config).getCache(randomUuid) }

        // then: Not Found exception is thrown
        exception.statusCode shouldBe 404
        exception.responseBody shouldContain "\"message\":\"Resource Not Found: uuid $cachePrefix$randomUuid\""
    }

    should("rethrow an exception from Aerospike cache server when such happens") {
        // given: Prebid Cache with not matched to Aerospike server namespace
        val unmatchedNamespace = PrebidCacheUtil.getRandomString()
        val config = prebidCacheConfig.getCacheExpiryConfig() + prebidCacheConfig.getAerospikeConfig(unmatchedNamespace)
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(config)

        // and: Default request object
        val requestObject = RequestObject.getDefaultJsonRequestObject()

        // when: POST cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject) }

        // then: Internal Server Error exception is thrown
        exception.statusCode shouldBe 500
        exception.responseBody shouldContain "Namespace not found in partition map: $unmatchedNamespace"

        // cleanup
        ContainerDependencies.prebidCacheContainerPool.stopPrebidCacheContainer(config)
    }

    should("throw an exception when aerospike.prevent_UUID_duplication=true and request with already existing UUID is send") {
        // given: Prebid Cache with aerospike.prevent_UUID_duplication=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(prebidCacheConfig.getBaseAerospikeConfig("true") +
                prebidCacheConfig.getAerospikePreventUuidDuplicationConfig("true"))

        // and: First request object with set UUID
        val uuid = PrebidCacheUtil.getRandomUuid()
        val xmlPayloadTransfer = PayloadTransfer.getDefaultXmlPayloadTransfer()
        xmlPayloadTransfer.key = uuid
        val requestObject = RequestObject(listOf(xmlPayloadTransfer))

        // and: First request object is saved to Aerospike cache
        prebidCacheApi.postCache(requestObject)

        // and: Second request object with already existing UUID is prepared
        val jsonPayloadTransfer = PayloadTransfer.getDefaultJsonPayloadTransfer()
        jsonPayloadTransfer.key = uuid
        val secondRequestObject = RequestObject(listOf(jsonPayloadTransfer))

        // when: POST cache endpoint is called for the second time
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.postCache(secondRequestObject) }

        // then: Bad Request exception is thrown
        exception.statusCode shouldBe 400
        exception.responseBody shouldContain "\"message\":\"UUID duplication.\""
    }

    should("return back two request UUIDs when allow_external_UUID=true and 2 payload transfers were successfully cached in Aerospike") {
        // given: Prebid Cache with allow_external_UUID=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(prebidCacheConfig.getBaseAerospikeConfig("true") +
                prebidCacheConfig.getAerospikePreventUuidDuplicationConfig("false"))

        // and: Request object with 2 payload transfers and set UUIDs is prepared
        val xmlPayloadTransfer = PayloadTransfer.getDefaultXmlPayloadTransfer()
        val jsonPayloadTransfer = PayloadTransfer.getDefaultJsonPayloadTransfer()
        xmlPayloadTransfer.key = PrebidCacheUtil.getRandomUuid()
        jsonPayloadTransfer.key = PrebidCacheUtil.getRandomUuid()
        val requestObject = RequestObject(listOf(xmlPayloadTransfer, jsonPayloadTransfer))

        // when: POST cache endpoint is called
        val responseObject: ResponseObject = prebidCacheApi.postCache(requestObject)

        // then: UUIDs from request object are returned
        responseObject.responses.isEmpty() shouldBe false
        responseObject.responses.size shouldBe 2

        val responseUuidList = listOf(responseObject.responses[0].uuid, responseObject.responses[1].uuid)
        responseUuidList shouldContain requestObject.puts[0].key
        responseUuidList shouldContain requestObject.puts[1].key
    }

    should("update existing cache record when aerospike.prevent_UUID_duplication=false and request with already existing UUID is send") {
        // given: Prebid Cache with aerospike.prevent_UUID_duplication=false
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(prebidCacheConfig.getBaseAerospikeConfig("true") +
                prebidCacheConfig.getAerospikePreventUuidDuplicationConfig("false"))

        // and: First request object
        val uuid = PrebidCacheUtil.getRandomUuid()
        val xmlPayloadTransfer = PayloadTransfer.getDefaultXmlPayloadTransfer()
        xmlPayloadTransfer.key = uuid
        val requestObject = RequestObject(listOf(xmlPayloadTransfer))

        // and: First request object is saved to Aerospike cache
        prebidCacheApi.postCache(requestObject)

        // and: Second request object with already existing UUID is prepared
        val jsonPayloadTransfer = PayloadTransfer.getDefaultJsonPayloadTransfer()
        jsonPayloadTransfer.key = uuid
        val secondRequestObject = RequestObject(listOf(jsonPayloadTransfer))
        val requestTransferValue = PrebidCacheUtil.objectMapper.readValue(secondRequestObject.puts[0].value, TransferValue::class.java)

        // when: POST cache endpoint is called for the second time
        val responseObject = prebidCacheApi.postCache(secondRequestObject)

        // then: UUID from request is returned
        responseObject.responses.isEmpty() shouldBe false
        responseObject.responses.size shouldBe 1
        responseObject.responses[0].uuid shouldBe secondRequestObject.puts[0].key

        // and: Cache record was updated in Aerospike with a second request object payload
        val getCacheResponse = prebidCacheApi.getCache(responseObject.responses[0].uuid)
        val responseTransferValue = getCacheResponse.`as`(TransferValue::class.java)

        responseTransferValue.adm shouldBe requestTransferValue.adm
        responseTransferValue.width shouldBe requestTransferValue.width
        responseTransferValue.height shouldBe requestTransferValue.height
    }
})
