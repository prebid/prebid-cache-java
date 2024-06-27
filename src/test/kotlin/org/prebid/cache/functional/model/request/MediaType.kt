package org.prebid.cache.functional.model.request

import com.fasterxml.jackson.annotation.JsonValue

enum class MediaType {

    JSON, XML, TEXT, UNSUPPORTED;

    @JsonValue
    fun getValue(): String = name.lowercase()
}
