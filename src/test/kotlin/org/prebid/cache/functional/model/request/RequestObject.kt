package org.prebid.cache.functional.model.request

data class RequestObject(var puts: List<PayloadTransfer>) {

    companion object {
        fun getDefaultJsonRequestObject(): RequestObject =
            RequestObject(puts = listOf(PayloadTransfer.getDefaultJsonPayloadTransfer()))

        fun getDefaultXmlRequestObject(): RequestObject =
            RequestObject(puts = listOf(PayloadTransfer.getDefaultXmlPayloadTransfer()))
    }
}
