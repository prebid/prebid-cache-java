package org.prebid.cache.functional.util

import java.util.UUID

fun getRandomUuid(): String = UUID.randomUUID().toString()

fun getRandomString(length: Int = 16): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length).map { allowedChars.random() }
        .joinToString("")
}
