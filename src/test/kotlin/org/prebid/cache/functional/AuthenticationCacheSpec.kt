package org.prebid.cache.functional

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.beEmpty
import io.kotest.matchers.string.shouldContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import org.prebid.cache.functional.BaseSpec.Companion.prebidCacheConfig
import org.prebid.cache.functional.mapper.objectMapper
import org.prebid.cache.functional.model.request.RequestObject
import org.prebid.cache.functional.model.request.TransferValue
import org.prebid.cache.functional.service.ApiException
import org.prebid.cache.functional.util.getRandomString
import org.prebid.cache.functional.util.getRandomUuid
import org.springframework.http.HttpStatus.UNAUTHORIZED

class AuthenticationCacheSpec : ShouldSpec({

    should("should save JSON transfer value without api-key in header when cache-write-secured is disabled") {
        // given: Prebid Cache with api.cache-write-secured=false property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = false,
                apiKey = getRandomString()
            )
        )

        // and: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()
        val requestTransferValue = objectMapper.readValue(requestObject.puts[0].value, TransferValue::class.java)

        // when: POST cache endpoint is called
        val postResponse = prebidCacheApi.postCache(requestObject, apiKey = null)

        // when: GET cache endpoint is called
        val getCacheResponse = BaseSpec.getPrebidCacheApi().getCache(postResponse.responses[0].uuid)

        // then: response content type is the same as request object type
        getCacheResponse.contentType()?.contentType shouldBe "application"
        getCacheResponse.contentType()?.contentSubtype shouldBe requestObject.puts[0].type.getValue()

        // and: transfer value is returned
        val responseTransferValue = objectMapper.readValue(getCacheResponse.bodyAsText(), TransferValue::class.java)

        assertSoftly {
            responseTransferValue.adm shouldBe requestTransferValue.adm
            responseTransferValue.width shouldBe requestTransferValue.width
            responseTransferValue.height shouldBe requestTransferValue.height
        }
    }

    should("should save JSON transfer value with proper api-key in header when cache-write-secured is enabled") {
        // given: Prebid Cache with api.cache-write-secured=true property
        val prebidApiKey = getRandomString()
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = true,
                apiKey = prebidApiKey
            )
        )

        // and: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()
        val requestTransferValue = objectMapper.readValue(requestObject.puts[0].value, TransferValue::class.java)

        // when: POST cache endpoint is called
        val postResponse = prebidCacheApi.postCache(requestObject, apiKey = prebidApiKey)

        // when: GET cache endpoint is called
        val getCacheResponse = BaseSpec.getPrebidCacheApi().getCache(postResponse.responses[0].uuid)

        // then: response content type is the same as request object type
        getCacheResponse.contentType()?.contentType shouldBe "application"
        getCacheResponse.contentType()?.contentSubtype shouldBe requestObject.puts[0].type.getValue()

        // and: transfer value is returned
        val responseTransferValue = objectMapper.readValue(getCacheResponse.bodyAsText(), TransferValue::class.java)

        assertSoftly {
            responseTransferValue.adm shouldBe requestTransferValue.adm
            responseTransferValue.width shouldBe requestTransferValue.width
            responseTransferValue.height shouldBe requestTransferValue.height
        }
    }

    should("should throw exception when cache-write-secured is enabled and trying to save payload transfer without api-key") {
        // given: Prebid Cache with api.cache-write-secured=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = true,
                apiKey = getRandomString()
            )
        )

        // and: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()

        // when: POST cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject, apiKey = null) }

        // then: Unauthorized exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNAUTHORIZED.value()
            exception.responseBody should beEmpty()
        }
    }

    should("should throw exception when cache-write-secured is enabled and trying to save payload transfer with empty api-key") {
        // given: Prebid Cache with api.cache-write-secured=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = true,
                apiKey = getRandomString()
            )
        )

        // and: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()

        // when: POST cache endpoint is called
        val exception =
            shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject, apiKey = "") }

        // then: Unauthorized exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNAUTHORIZED.value()
            exception.responseBody should beEmpty()
        }
    }

    should("should throw exception when cache-write-secured is enabled and trying to save payload transfer with invalid api-key") {
        // given: Prebid Cache with api.cache-write-secured=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = true,
                apiKey = getRandomString()
            )
        )

        // and: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()

        // when: POST cache endpoint is called
        val exception =
            shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject, apiKey = getRandomString()) }

        // then: Unauthorized exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNAUTHORIZED.value()
            exception.responseBody should beEmpty()
        }
    }

    should("should throw exception when cache-write-secured is enabled and trying to save payload transfer with different case strategy api-key") {
        // given: Prebid Cache with api.cache-write-secured=true property
        val prebidApiKey = getRandomString()
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = true,
                apiKey = prebidApiKey
            )
        )

        // and: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()

        // when: POST cache endpoint is called
        val exception =
            shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject, apiKey = prebidApiKey.uppercase()) }

        // then: Unauthorized exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNAUTHORIZED.value()
            exception.responseBody should beEmpty()
        }
    }

    should("should save JSON transfer value with proper api-key in header when cache-write-secured and external-uuid-secured are enabled") {
        // given: Prebid Cache with external-uuid-secured=true property
        val prebidApiKey = getRandomString()
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = true,
                externalUuidSecured = true,
                apiKey = prebidApiKey
            )
        )

        // and: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()
        val requestTransferValue = objectMapper.readValue(requestObject.puts[0].value, TransferValue::class.java)

        // when: POST cache endpoint is called
        val postResponse = prebidCacheApi.postCache(requestObject, apiKey = prebidApiKey)

        // when: GET cache endpoint is called
        val getCacheResponse = BaseSpec.getPrebidCacheApi().getCache(postResponse.responses[0].uuid)

        // then: response content type is the same as request object type
        getCacheResponse.contentType()?.contentType shouldBe "application"
        getCacheResponse.contentType()?.contentSubtype shouldBe requestObject.puts[0].type.getValue()

        // and: transfer value is returned
        val responseTransferValue = objectMapper.readValue(getCacheResponse.bodyAsText(), TransferValue::class.java)

        assertSoftly {
            responseTransferValue.adm shouldBe requestTransferValue.adm
            responseTransferValue.width shouldBe requestTransferValue.width
            responseTransferValue.height shouldBe requestTransferValue.height
        }
    }

    should("should save JSON transfer value with proper api-key in header when cache-write-secured disabled and external-uuid-secured enabled") {
        // given: Prebid Cache with external-uuid-secured=true property
        val prebidApiKey = getRandomString()
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = false,
                externalUuidSecured = true,
                apiKey = prebidApiKey
            )
        )

        // and: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()
        val requestTransferValue = objectMapper.readValue(requestObject.puts[0].value, TransferValue::class.java)

        // when: POST cache endpoint is called
        val postResponse = prebidCacheApi.postCache(requestObject, apiKey = prebidApiKey)

        // when: GET cache endpoint is called
        val getCacheResponse = BaseSpec.getPrebidCacheApi().getCache(postResponse.responses[0].uuid)

        // then: response content type is the same as request object type
        getCacheResponse.contentType()?.contentType shouldBe "application"
        getCacheResponse.contentType()?.contentSubtype shouldBe requestObject.puts[0].type.getValue()

        // and: transfer value is returned
        val responseTransferValue = objectMapper.readValue(getCacheResponse.bodyAsText(), TransferValue::class.java)

        assertSoftly {
            responseTransferValue.adm shouldBe requestTransferValue.adm
            responseTransferValue.width shouldBe requestTransferValue.width
            responseTransferValue.height shouldBe requestTransferValue.height
        }
    }

    should("should save JSON transfer value with proper api-key in header when cache-write-secured and external-uuid-secured are disabled") {
        // given: Prebid Cache with external-uuid-secured=false property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = false,
                externalUuidSecured = false,
                apiKey = getRandomString()
            )
        )

        // and: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()
        val requestTransferValue = objectMapper.readValue(requestObject.puts[0].value, TransferValue::class.java)

        // when: POST cache endpoint is called
        val postResponse = prebidCacheApi.postCache(requestObject)

        // when: GET cache endpoint is called
        val getCacheResponse = BaseSpec.getPrebidCacheApi().getCache(postResponse.responses[0].uuid)

        // then: response content type is the same as request object type
        getCacheResponse.contentType()?.contentType shouldBe "application"
        getCacheResponse.contentType()?.contentSubtype shouldBe requestObject.puts[0].type.getValue()

        // and: transfer value is returned
        val responseTransferValue = objectMapper.readValue(getCacheResponse.bodyAsText(), TransferValue::class.java)

        assertSoftly {
            responseTransferValue.adm shouldBe requestTransferValue.adm
            responseTransferValue.width shouldBe requestTransferValue.width
            responseTransferValue.height shouldBe requestTransferValue.height
        }
    }

    should("throw an exception when api key is missing for request cache_write_secured=true and external_uuid_Secured=true") {
        // given: Prebid Cache with external-uuid-secured=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = true,
                externalUuidSecured = true,
                apiKey = getRandomString()
            )
        )

        // and: Request object with set payload transfer key
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply { puts[0].key = getRandomUuid() }

        // when: POST cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject) }

        // then: Unauthorized exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNAUTHORIZED.value()
            exception.responseBody should beEmpty()
        }
    }

    should("throw an exception when api key is missing for request and external_uuid_Secured=true") {
        // given: Prebid Cache with external-uuid-secured=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = false,
                externalUuidSecured = true,
                apiKey = getRandomString()
            )
        )

        // and: Request object with set payload transfer key
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply { puts[0].key = getRandomUuid() }

        // when: POST cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject) }

        // then: Unauthorized exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNAUTHORIZED.value()
            exception.responseBody shouldContain "\"message\":\"Prebid cache host forbids specifying UUID in request by unauthorized users.\""
        }
    }

    should("throw an exception when api key is mismatched for request and external-uuid-secured=true") {
        // given: Prebid Cache with external-uuid-secured=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            prebidCacheConfig.getBaseRedisConfig(
                allowExternalUuid = true,
                cacheWriteSecured = false,
                externalUuidSecured = true,
                apiKey = getRandomString()
            )
        )

        // and: Request object with set payload transfer key
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply { puts[0].key = getRandomUuid() }

        // when: POST cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject, apiKey = getRandomString()) }

        // then: Unauthorized exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNAUTHORIZED.value()
            exception.responseBody shouldContain "\"message\":\"Prebid cache host forbids specifying UUID in request by unauthorized users.\""
        }
    }
})
