package org.prebid.cache.functional.service

data class ApiException(val statusCode: Int, val responseBody: String) : Exception()
