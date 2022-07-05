package org.prebid.cache.functional.model.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import org.prebid.cache.functional.mapper.objectMapper
import org.prebid.cache.functional.model.request.MediaType.JSON
import org.prebid.cache.functional.model.request.MediaType.XML

@JsonInclude(NON_NULL)
data class PayloadTransfer(
    var type: MediaType,
    var value: String,
    var key: String? = null,
    var expiry: Long? = null,
    var ttlseconds: Long? = null,
    var prefix: String? = null,
    var timestamp: Long? = null
) {

    companion object {
        fun getDefaultJsonPayloadTransfer(): PayloadTransfer =
            PayloadTransfer(
                type = JSON,
                value = objectMapper.writeValueAsString(TransferValue.getDefaultJsonValue()),
                expiry = 300
            )

        fun getDefaultXmlPayloadTransfer(): PayloadTransfer =
            PayloadTransfer(
                type = XML,
                value = objectMapper.writeValueAsString(TransferValue.getDefaultXmlValue()),
                expiry = 300
            )
    }
}
