package org.prebid.cache.functional.model.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import org.prebid.cache.functional.model.request.MediaType.JSON
import org.prebid.cache.functional.model.request.MediaType.XML
import org.prebid.cache.functional.util.PrebidCacheUtil

@JsonInclude(NON_NULL)
data class PayloadTransfer(
        var type: MediaType,
        var key: String? = null,
        var value: String,
        var expiry: Long? = null,
        var ttlseconds: Long? = null,
        var prefix: String? = null
) {

    companion object {
        fun getDefaultJsonPayloadTransfer(): PayloadTransfer {
            return PayloadTransfer(type = JSON,
                    value = PrebidCacheUtil.objectMapper.writeValueAsString(TransferValue.getDefaultJsonValue()),
                    expiry = 300)
        }

        fun getDefaultXmlPayloadTransfer(): PayloadTransfer {
            return PayloadTransfer(type = XML,
                    value = PrebidCacheUtil.objectMapper.writeValueAsString(TransferValue.getDefaultXmlValue()),
                    expiry = 300)
        }
    }
}
