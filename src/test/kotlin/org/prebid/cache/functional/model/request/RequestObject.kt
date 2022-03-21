package org.prebid.cache.functional.model.request

data class RequestObject(var puts: List<PayloadTransfer>) {

    companion object {
        fun getDefaultJsonRequestObject(): RequestObject {
            return RequestObject(puts = listOf(PayloadTransfer.getDefaultJsonPayloadTransfer()))
        }

        fun getDefaultXmlRequestObject(): RequestObject {
            return RequestObject(puts = listOf(PayloadTransfer.getDefaultXmlPayloadTransfer()))
        }
    }
}
