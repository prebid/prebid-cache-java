package org.prebid.cache.functional.util

import java.util.UUID
import java.util.Random

fun getRandomUuid(): String = UUID.randomUUID().toString()

fun getRandomString(length: Int = 16): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return List(length) { allowedChars.random() }.joinToString("")
}

fun getRandomLong(start: Int = 0, end: Int = 16): Long = Random().nextInt(start, end).toLong()
