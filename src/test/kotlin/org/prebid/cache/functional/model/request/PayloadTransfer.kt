package org.prebid.cache.functional.model.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import org.prebid.cache.functional.mapper.objectMapper
import org.prebid.cache.functional.model.request.MediaType.JSON
import org.prebid.cache.functional.model.request.MediaType.TEXT
import org.prebid.cache.functional.model.request.MediaType.XML
import org.prebid.cache.functional.util.getRandomString

@JsonInclude(NON_NULL)
data class PayloadTransfer(
    var type: MediaType,
    var value: String,
    var key: String? = null,
    var expiry: Long? = null,
    var ttlseconds: Long? = null,
    var prefix: String? = null,
    var application: String? = null,

    //Fields not affected on PBC
    var timestamp: Long? = null,
    var bidder: String? = null,
    var bidid: String? = null,
    var aid: String? = null,
) {

    companion object {
        private const val DEFAULT_EXPIRY = 300L

        private fun getDefaultPayloadTransfer(type: MediaType, value: String): PayloadTransfer =
            PayloadTransfer(type = type, value = value, expiry = DEFAULT_EXPIRY, ttlseconds = 3000L)

        fun getDefaultJsonPayloadTransfer(): PayloadTransfer =
            getDefaultPayloadTransfer(JSON, objectMapper.writeValueAsString(TransferValue.getDefaultJsonValue()))

        fun getDefaultXmlPayloadTransfer(): PayloadTransfer =
            getDefaultPayloadTransfer(XML, objectMapper.writeValueAsString(TransferValue.getDefaultXmlValue()))

        fun getDefaultTextPayloadTransfer(): PayloadTransfer =
            getDefaultPayloadTransfer(TEXT, getRandomString())
    }
}
