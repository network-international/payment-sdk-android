package payment.sdk.android.cardpayment

class CardPaymentRequest private constructor(
        val gatewayUrl: String,
        val code: String
) {

    class Builder {
        private var gatewayUrl: String? = null
        private var code: String? = null

        fun gatewayUrl(url: String) = this.apply {
            gatewayUrl = url
        }

        fun code(c: String) = this.apply {
            code = c
        }

        fun build(): CardPaymentRequest {
            requireNotNull(gatewayUrl) {
                "Gateway url should not be null"
            }
            requireNotNull(code) {
                "Code should not be null"
            }
            return CardPaymentRequest(gatewayUrl!!, code!!)

        }
    }

    companion object {
        fun builder() = Builder()
    }

}