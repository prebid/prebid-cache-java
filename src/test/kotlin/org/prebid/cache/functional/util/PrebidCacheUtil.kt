package org.prebid.cache.functional.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.util.*

class PrebidCacheUtil {

    companion object {
        val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

        fun getRandomUuid(): String {
            return UUID.randomUUID().toString()
        }

        fun getRandomString(length: Int = 16): String {
            val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..length).map { allowedChars.random() }
                    .joinToString("")
        }
    }
}
