package payment.sdk.android.savedCard

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

/**
 * [SavedCardPaymentRequest] holds the data required to initiate a saved card payment.
 *
 * This request supports configuring essential details, such as authorization URL, payment URL
 * and optional CVV.
 *
 * Example:
 * ```
 * val savedCardPaymentRequest = SavedCardPaymentRequest.builder()
 *     .gatewayAuthorizationUrl(authUrl)
 *     .payPageUrl(payPageUrl)
 *     .setCvv(cvv)
 *     .build()
 * ```
 *
 * @property authorizationUrl The URL to authorize the payment gateway.
 * @property paymentUrl The URL to the payment page.
 * @property cvv The CVV for the saved card (optional).
 * @constructor Creates a new instance of `SavedCardPaymentRequest`.
 */
@Parcelize
class SavedCardPaymentRequest private constructor(
    val authorizationUrl: String,
    val paymentUrl: String,
    val cvv: String?
) : Parcelable {

    /**
     * Builder class for [SavedCardPaymentRequest].
     */
    class Builder {
        private lateinit var _authorizationUrl: String
        private lateinit var _paymentUrl: String
        private var _language: String = "en"
        private var _cvv: String? = null

        /**
         * Sets the gateway authorization URL.
         *
         * @param url The URL for the payment gateway authorization.
         * @return The builder instance.
         */
        fun gatewayAuthorizationUrl(url: String) = apply {
            this._authorizationUrl = url
        }

        /**
         * Sets the payment page URL.
         *
         * @param url The URL for the payment page.
         * @return The builder instance.
         */
        fun payPageUrl(url: String) = apply {
            this._paymentUrl = url
        }

        /**
         * Sets the CVV for the saved card.
         *
         * @param cvv The CVV for the saved card.
         * @return The builder instance.
         */
        fun setCvv(cvv: String) = apply {
            this._cvv = cvv
        }

        /**
         * Builds the [SavedCardPaymentRequest] instance.
         *
         * @return An instance of `SavedCardPaymentRequest`.
         * @throws IllegalArgumentException if required fields are not initialized.
         */
        fun build(): SavedCardPaymentRequest {
            check(this::_authorizationUrl.isInitialized || _authorizationUrl.isEmpty()) { "Gateway url should not be null" }
            check(this::_paymentUrl.isInitialized || _paymentUrl.isEmpty()) { "Pay page url should not be null" }
            return SavedCardPaymentRequest(
                authorizationUrl = _authorizationUrl,
                paymentUrl = _paymentUrl,
                cvv = _cvv
            )
        }
    }

    private fun toBundle() = bundleOf(EXTRA_ARGS to this)

    fun toIntent(context: Context) = Intent(
        context,
        SavedCardPaymentActivity::class.java
    ).apply {
        putExtra(
            EXTRA_INTENT,
            toBundle()
        )
    }

    companion object {
        /**
         * Creates a new builder instance for `SavedCardPaymentRequest`.
         *
         * @return A new `SavedCardPaymentRequest.Builder` instance.
         */
        fun builder() = Builder()

        private const val EXTRA_ARGS = "saved_card_payments_request_args"
        private const val EXTRA_INTENT = "saved_card_payments_request_args_intent"

        fun fromIntent(intent: Intent): SavedCardPaymentRequest? {
            val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
            return inputIntent?.getParcelable(EXTRA_ARGS)
        }
    }
}