package payment.sdk.android.payments

class PaymentsRequest private constructor(
    val authorizationUrl: String,
    val paymentUrl: String
) {
    class Builder {
        private lateinit var _authorizationUrl: String
        private lateinit var _paymentUrl: String

        fun gatewayAuthorizationUrl(url: String) = apply {
            this._authorizationUrl = url
        }

        fun payPageUrl(url: String) = apply {
            this._paymentUrl = url
        }

        fun build(): PaymentsRequest {
            check(this::_authorizationUrl.isInitialized) { "Gateway url should not be null" }
            check(this::_paymentUrl.isInitialized) { "Pay page url should not be null" }
            return PaymentsRequest(_authorizationUrl, _paymentUrl)
        }
    }

    companion object {
        fun builder() = Builder()
    }
}