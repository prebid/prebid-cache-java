package org.prebid.cache.functional

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.beEmpty
import io.kotest.matchers.string.shouldContain
import java.util.Locale
import org.prebid.cache.functional.BaseSpec.Companion.prebidCacheConfig
import org.prebid.cache.functional.model.request.PayloadTransfer
import org.prebid.cache.functional.service.ApiException
import org.prebid.cache.functional.service.PrebidCacheApi
import org.prebid.cache.functional.util.getRandomString
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED

class ModuleStorageSpec : ShouldSpec({

    lateinit var apiKey: String
    lateinit var applicationName: String
    lateinit var cacheApi: PrebidCacheApi

    beforeSpec {
        apiKey = getRandomString()
        applicationName = getRandomString().lowercase(Locale.getDefault())
        val config = prebidCacheConfig.getBaseModuleStorageConfig(applicationName, apiKey)
        cacheApi = BaseSpec.getPrebidCacheApi(config)
    }

    should("return the same text transfer value which was saved to module-storage") {
        //given: default text payload with application
        val payloadKey = getRandomString()
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = payloadKey
            application = applicationName
        }

        // when: POST module-storage endpoint is called
        cacheApi.postModuleStorageCache(payloadTransfer, apiKey)

        // then: recorded payload should contain the same type and value
        val savedPayload = cacheApi.getModuleStorageCache(payloadKey, applicationName, apiKey)
        savedPayload.type shouldBe payloadTransfer.type
        savedPayload.value shouldBe payloadTransfer.value

        // and: shouldn't contain information about application
        savedPayload.application?.should(beNull())
    }

    should("return the same xml transfer value which was saved to module-storage") {
        //given: default xml payload with application
        val payloadKey = getRandomString()
        val payloadTransfer = PayloadTransfer.getDefaultXmlPayloadTransfer().apply {
            key = payloadKey
            application = applicationName
        }

        // when: POST module-storage endpoint is called
        cacheApi.postModuleStorageCache(payloadTransfer, apiKey)

        // then: recorded payload should contain the same type and value
        val savedPayload = cacheApi.getModuleStorageCache(payloadKey, applicationName, apiKey)
        savedPayload.type shouldBe payloadTransfer.type
        savedPayload.value shouldBe payloadTransfer.value

        // and: shouldn't contain information about application
        savedPayload.application?.should(beNull())
    }

    should("return the same json transfer value which was saved to module-storage") {
        //given: default json payload with application
        val payloadKey = getRandomString()
        val payloadTransfer = PayloadTransfer.getDefaultJsonPayloadTransfer().apply {
            key = payloadKey
            application = applicationName
        }

        // when: POST module-storage endpoint is called
        cacheApi.postModuleStorageCache(payloadTransfer, apiKey)

        // then: recorded payload should contain the same type and value
        val savedPayload = cacheApi.getModuleStorageCache(payloadKey, applicationName, apiKey)
        savedPayload.type shouldBe payloadTransfer.type
        savedPayload.value shouldBe payloadTransfer.value

        // and: shouldn't contain information about application
        savedPayload.application?.should(beNull())
    }

    should("throw an exception when post request have nonexistent PBC application name") {
        //given: default text payload with nonexistent application name
        val payloadKey = getRandomString()
        val randomApplication = getRandomString()
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = payloadKey
            application = randomApplication
        }

        // when: POST module-storage endpoint is called
        val exception = shouldThrowExactly<ApiException> {
            cacheApi.postModuleStorageCache(payloadTransfer, apiKey) }

        // then: Not found exception is thrown
        assertSoftly {
            exception.statusCode shouldBe NOT_FOUND.value()
            exception.responseBody shouldContain "\"path\":\"/module-storage\""
            exception.responseBody shouldContain "\"message\":\"Invalid application: ${randomApplication}\""
        }
    }

    should("throw an exception when post request have null application name") {
        //given: default text payload with null application name
        val payloadKey = getRandomString()
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = payloadKey
            application = null
        }

        // when: POST module-storage endpoint is called
        val exception = shouldThrowExactly<ApiException> { cacheApi.postModuleStorageCache(payloadTransfer, apiKey) }

        // then: Bad request exception is thrown
        assertSoftly {
            exception.statusCode shouldBe BAD_REQUEST.value()
            exception.responseBody shouldContain "\"path\":\"/module-storage\""
            exception.responseBody shouldContain "application must not be empty"
        }
    }

    should("throw an exception when post request have empty application name") {
        //given: default text payload with empty application name
        val payloadKey = getRandomString()
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = payloadKey
            application = ""
        }

        // when: POST module-storage endpoint is called
        val exception = shouldThrowExactly<ApiException> { cacheApi.postModuleStorageCache(payloadTransfer, apiKey) }

        // then: Bad request exception is thrown
        assertSoftly {
            exception.statusCode shouldBe BAD_REQUEST.value()
            exception.responseBody shouldContain "\"path\":\"/module-storage\""
            exception.responseBody shouldContain "application must not be empty"
        }
    }

    should("throw an exception when post request have null key name") {
        //given: default text payload with empty payloadKey
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = null
            application = applicationName
        }

        // when: POST module-storage endpoint is called
        val exception = shouldThrowExactly<ApiException> { cacheApi.postModuleStorageCache(payloadTransfer, apiKey) }

        // then: Bad request exception is thrown
        assertSoftly {
            exception.statusCode shouldBe BAD_REQUEST.value()
            exception.responseBody shouldContain "\"path\":\"/module-storage\""
            exception.responseBody shouldContain "key must not be empty"
        }
    }

    should("throw an exception when post request have empty key name") {
        //given: default text payload with empty payloadKey
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = ""
            application = applicationName
        }

        // when: POST module-storage endpoint is called
        val exception = shouldThrowExactly<ApiException> { cacheApi.postModuleStorageCache(payloadTransfer, apiKey) }

        // then: Bad request exception is thrown
        assertSoftly {
            exception.statusCode shouldBe BAD_REQUEST.value()
            exception.responseBody shouldContain "\"path\":\"/module-storage\""
            exception.responseBody shouldContain "key must not be empty"
        }
    }

    should("throw an exception when post request have invalid PBC apiKey") {
        //given: default text payload with application
        val payloadKey = getRandomString()
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = payloadKey
            application = applicationName
        }

        // when: POST module-storage endpoint is called
        val exception =
            shouldThrowExactly<ApiException> { cacheApi.postModuleStorageCache(payloadTransfer, getRandomString()) }

        // then: Not found exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNAUTHORIZED.value()
            exception.responseBody should beEmpty()
        }
    }

    should("throw an exception when get request contain invalid payload key") {
        //given: default text payload with application
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = getRandomString()
            application = applicationName
        }

        // and: POST module-storage endpoint is called
        cacheApi.postModuleStorageCache(payloadTransfer, apiKey)

        // when: GET module-storage endpoint is called with invalid data
        val exception = shouldThrowExactly<ApiException> {
            cacheApi.getModuleStorageCache(getRandomString(), applicationName, apiKey)
        }

        // then: Not found exception is thrown
        assertSoftly {
            exception.statusCode shouldBe NOT_FOUND.value()
            exception.responseBody shouldContain "\"path\":\"/module-storage\""
            exception.responseBody shouldContain "Invalid application or key"
        }
    }

    should("throw an exception when get request contain invalid application name") {
        //given: default text payload with application
        val payloadKey = getRandomString()
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = payloadKey
            application = applicationName
        }

        // and: POST module-storage endpoint is called
        cacheApi.postModuleStorageCache(payloadTransfer, apiKey)

        //and: random application name
        val randomApplication = getRandomString()

        // when: GET module-storage endpoint is called with invalid data
        val exception = shouldThrowExactly<ApiException> {
            cacheApi.getModuleStorageCache(payloadKey, randomApplication, apiKey)
        }

        // then: Not found exception is thrown
        assertSoftly {
            exception.statusCode shouldBe NOT_FOUND.value()
            exception.responseBody shouldContain "\"path\":\"/module-storage\""
            exception.responseBody shouldContain "\"message\":\"Invalid application: ${randomApplication}\""
        }
    }

    should("throw an exception when get request contain invalid apiKey") {
        //given: default text payload with application
        val payloadKey = getRandomString()
        val payloadTransfer = PayloadTransfer.getDefaultTextPayloadTransfer().apply {
            key = payloadKey
            application = applicationName
        }

        // and: POST module-storage endpoint is called
        cacheApi.postModuleStorageCache(payloadTransfer, apiKey)

        // when: GET module-storage endpoint is called with invalid data
        val exception = shouldThrowExactly<ApiException> {
            cacheApi.getModuleStorageCache(payloadKey, applicationName, getRandomString())
        }

        // then: Not found exception is thrown
        assertSoftly {
            exception.statusCode shouldBe UNAUTHORIZED.value()
            exception.responseBody should beEmpty()
        }
    }
})
