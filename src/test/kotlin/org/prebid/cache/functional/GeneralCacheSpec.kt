package org.prebid.cache.functional

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.prebid.cache.functional.mapper.objectMapper
import org.prebid.cache.functional.model.request.MediaType.UNSUPPORTED
import org.prebid.cache.functional.model.request.RequestObject
import org.prebid.cache.functional.model.request.TransferValue
import org.prebid.cache.functional.service.ApiException
import org.prebid.cache.functional.util.getRandomString
import org.prebid.cache.functional.util.getRandomUuid
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE

class GeneralCacheSpec : ShouldSpec({

    should("throw an exception when 'uuid' query parameter is not present in request") {
        // when: GET cache endpoint is called without 'uuid' query parameter
        val exception = shouldThrowExactly<ApiException> { BaseSpec.getPrebidCacheApi().getCache(null) }

        // then: Bad Request exception is thrown
        assertSoftly {
            exception.statusCode shouldBe BAD_REQUEST.value()
            exception.responseBody shouldContain "\"message\":\"Invalid Parameter(s): uuid not found.\""
        }
    }

    should("throw an exception when payload transfer key is given and allow_external_UUID=false") {
        // given: Request object with set payload transfer key
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply { puts[0].key = getRandomUuid() }

        // when: POST cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { BaseSpec.getPrebidCacheApi().postCache(requestObject) }

        // then: Bad Request exception is thrown
        assertSoftly {
            exception.statusCode shouldBe BAD_REQUEST.value()
            exception.responseBody shouldContain "\"message\":\"Prebid cache host forbids specifying UUID in request.\""
        }
    }

    should("throw an exception when allow_external_UUID=true and payload transfer key not in UUID format is given") {
        // given: Prebid Cache with allow_external_UUID=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(BaseSpec.prebidCacheConfig.getBaseRedisConfig("true"))

        // and: Request object with set payload transfer key not in UUID format
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply { puts[0].key = getRandomUuid() + "*" }

        // when: POST cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject) }

        // then: Bad Request exception is thrown
        assertSoftly {
            exception.statusCode shouldBe BAD_REQUEST.value()
            exception.responseBody shouldContain "\"message\":\"Invalid UUID: [${requestObject.puts[0].key}].\""
        }
    }

    should("throw an exception when allow_external_UUID=true and empty payload transfer key is given") {
        // given: Prebid Cache with allow_external_UUID=true property
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(BaseSpec.prebidCacheConfig.getBaseRedisConfig("true"))

        // and: Request object with set empty payload transfer key
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply { puts[0].key = "" }

        // when: POST cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject) }

        // then: Bad Request exception is thrown
        assertSoftly {
            exception.statusCode shouldBe BAD_REQUEST.value()
            exception.responseBody shouldContain "\"message\":\"Invalid UUID: [].\""
        }
    }

    should("throw an exception when payload object with unsupported media type is fetched from repository") {
        // given: Request object with set unsupported media type
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply { puts[0].type = UNSUPPORTED }

        // and: POST cache endpoint is called
        val response = BaseSpec.getPrebidCacheApi().postCache(requestObject)

        // when: GET cache endpoint is called
        val exception =
            shouldThrowExactly<ApiException> { BaseSpec.getPrebidCacheApi().getCache(response.responses[0].uuid) }

        // then: Unsupported Media Type exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNSUPPORTED_MEDIA_TYPE.value()
            exception.responseBody shouldContain "\"message\":\"Unsupported Media Type.\""
        }
    }

    should("throw an exception on POST when request processing takes > than 'cache.timeout.ms' config property") {
        // given: Prebid Cache with a low cache request processing timeout ms
        val requestTimeoutMs = "1"
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            BaseSpec.prebidCacheConfig.getBaseRedisConfig("false") +
                    BaseSpec.prebidCacheConfig.getCacheTimeoutConfig(requestTimeoutMs)
        )

        // and: Request object
        val requestObject = RequestObject.getDefaultJsonRequestObject()

        // when: POST cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.postCache(requestObject) }

        // then: Internal Server Exception is thrown
        assertSoftly {
            exception.statusCode shouldBe INTERNAL_SERVER_ERROR.value()
            exception.responseBody shouldContain "\"message\":\"Did not observe any item or terminal signal within " +
                    "${requestTimeoutMs}ms in 'circuitBreaker' (and no fallback has been configured)\""
        }
    }

    should("throw an exception on GET when request processing takes > than 'cache.timeout.ms' config property") {
        // given: Prebid Cache with a low cache request processing timeout ms
        val requestTimeoutMs = "1"
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(
            BaseSpec.prebidCacheConfig.getBaseRedisConfig("false") +
                    BaseSpec.prebidCacheConfig.getCacheTimeoutConfig(requestTimeoutMs)
        )

        // and: POST cache endpoint is called
        val postResponse = BaseSpec.getPrebidCacheApi().postCache(RequestObject.getDefaultJsonRequestObject())

        // when: GET cache endpoint is called
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.getCache(postResponse.responses[0].uuid) }

        // then: Internal Server Exception is thrown
        assertSoftly {
            exception.statusCode shouldBe INTERNAL_SERVER_ERROR.value()
            exception.responseBody shouldContain "\"message\":\"Did not observe any item or terminal signal within " +
                    "${requestTimeoutMs}ms in 'circuitBreaker' (and no fallback has been configured)\""
        }
    }

    should("return the same JSON transfer value which was saved to cache") {
        // given: Request object with JSON transfer value
        val requestObject = RequestObject.getDefaultJsonRequestObject()
        val requestTransferValue = objectMapper.readValue(requestObject.puts[0].value, TransferValue::class.java)

        // and: POST cache endpoint is called
        val postResponse = BaseSpec.getPrebidCacheApi().postCache(requestObject)

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

    should("return the same XML transfer value which was saved to cache") {
        // given: Request object with XML transfer value
        val requestObject = RequestObject.getDefaultXmlRequestObject()
        val requestTransferValue = objectMapper.readValue(requestObject.puts[0].value, TransferValue::class.java)

        // and: POST cache endpoint is called
        val postResponse = BaseSpec.getPrebidCacheApi().postCache(requestObject)

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

    should("return the same String transfer value which was saved to cache") {
        // given: Request object with set transfer value as plain String
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply { puts[0].value = getRandomString() }

        // and: POST cache endpoint is called
        val postResponse = BaseSpec.getPrebidCacheApi().postCache(requestObject)

        // when: GET cache endpoint is called
        val getCacheResponse = BaseSpec.getPrebidCacheApi().getCache(postResponse.responses[0].uuid)

        // then: transfer value as a plain String is returned
        getCacheResponse.bodyAsText() shouldBe requestObject.puts[0].value
    }

    should("ignore fields that pass to cache and return ok status code") {
        // given: Request object with fields that should be ignored
        val requestObject = RequestObject.getDefaultJsonRequestObject().apply {
            puts[0].bidder = getRandomString()
            puts[0].aid = getRandomString()
            puts[0].bidid = getRandomString()
            puts[0].timestamp = 11L
        }

        // and: POST cache endpoint is called
        val postResponse = BaseSpec.getPrebidCacheApi().postCache(requestObject)

        // when: GET cache endpoint is called
        val getCacheResponse = BaseSpec.getPrebidCacheApi().getCache(postResponse.responses[0].uuid)

        // then: PBC should not fail
        getCacheResponse.status shouldBe HttpStatusCode.OK
    }
})
