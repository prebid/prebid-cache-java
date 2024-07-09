package org.prebid.cache.functional.model.request

data class RequestObject(var puts: List<PayloadTransfer>) {

    companion object {
        fun of(vararg puts: PayloadTransfer): RequestObject =
            RequestObject(puts = puts.asList())

        fun getDefaultJsonRequestObject(): RequestObject =
            RequestObject(puts = listOf(PayloadTransfer.getDefaultJsonPayloadTransfer()))

        fun getDefaultXmlRequestObject(): RequestObject =
            RequestObject(puts = listOf(PayloadTransfer.getDefaultXmlPayloadTransfer()))

        fun getDefaultTextRequestObject(): RequestObject =
            RequestObject(puts = listOf(PayloadTransfer.getDefaultTextPayloadTransfer()))
    }
}
