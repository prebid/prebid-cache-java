package org.prebid.cache.functional.util

import java.util.*

fun getRandomUuid(): String = UUID.randomUUID().toString()

fun getRandomString(length: Int = 16): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return List(length) { allowedChars.random() }.joinToString("")
}
