package payment.sdk.android.cardpayment

/**
 * `CardPaymentRequest` is deprecated and will be removed in future releases.
 * Use `UnifiedPaymentPageRequest` instead for improved flexibility and feature support.
 *
 * Migration:
 * ```
 * // Replace CardPaymentRequest usage with UnifiedPaymentPageRequest
 * val paymentsRequest = UnifiedPaymentPageRequest.builder()
 *     .gatewayAuthorizationUrl(authUrl)
 *     .payPageUrl(payPageUrl)
 *     .setLanguageCode(viewModel.getLanguageCode())
 *     .build()
 * ```
 *
 * @property gatewayUrl The URL for gateway authorization.
 * @property code The unique code for the payment request.
 * @see [UnifiedPaymentPageRequest]
 * @deprecated Use `UnifiedPaymentPageRequest` instead, which provides additional configuration options.
 */
@Deprecated("Use UnifiedPaymentPageRequest instead", ReplaceWith("UnifiedPaymentPageRequest"))
class CardPaymentRequest private constructor(
    val gatewayUrl: String,
    val code: String
) {

    /**
     * Builder class for `CardPaymentRequest`.
     * @deprecated Use `UnifiedPaymentPageRequest.Builder` instead.
     */
    @Deprecated("Use UnifiedPaymentPageRequest.Builder instead", ReplaceWith("UnifiedPaymentPageRequest.Builder"))
    class Builder {
        private var gatewayUrl: String? = null
        private var code: String? = null

        /**
         * Sets the gateway URL.
         *
         * @param url The URL for payment gateway authorization.
         * @return The builder instance.
         */
        fun gatewayUrl(url: String) = this.apply {
            gatewayUrl = url
        }

        /**
         * Sets the payment code.
         *
         * @param c The code for the payment request.
         * @return The builder instance.
         */
        fun code(c: String) = this.apply {
            code = c
        }

        /**
         * Builds the `CardPaymentRequest` instance.
         *
         * @return An instance of `CardPaymentRequest`.
         * @throws IllegalArgumentException if required fields are not initialized.
         */
        fun build(): CardPaymentRequest {
            requireNotNull(gatewayUrl) {
                "Gateway URL should not be null"
            }
            requireNotNull(code) {
                "Code should not be null"
            }
            return CardPaymentRequest(gatewayUrl!!, code!!)
        }
    }

    companion object {
        /**
         * Creates a new builder instance for `CardPaymentRequest`.
         *
         * @return A new `CardPaymentRequest.Builder` instance.
         */
        fun builder() = Builder()
    }
}