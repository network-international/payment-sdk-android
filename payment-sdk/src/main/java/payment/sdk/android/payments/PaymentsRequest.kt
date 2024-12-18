package payment.sdk.android.payments

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import payment.sdk.android.googlepay.GooglePayConfig

/**
 * `PaymentsRequest` class represents the request for launching the payment page.
 *
 * Use the `Builder` pattern to set necessary parameters and create an instance.
 *
 * Usage:
 * ```
 * val request = PaymentsRequest.builder()
 *     .gatewayAuthorizationUrl(authUrl)
 *     .payPageUrl(payPageUrl)
 *     .build()
 * ```
 *
 * @property authorizationUrl The authorization URL for gateway authentication.
 * @property paymentUrl The URL for the payment page.
 * @property googlePayConfig Optional configuration for Google Pay.
 */
@Parcelize
class PaymentsRequest private constructor(
    val authorizationUrl: String,
    val paymentUrl: String,
    val googlePayConfig: GooglePayConfig?
) : Parcelable {

    /**
     * Builder class for []PaymentsRequest].
     */
    class Builder {
        private lateinit var _authorizationUrl: String
        private lateinit var _paymentUrl: String
        private var _googlePayConfig: GooglePayConfig? = null

        /**
         * Sets the authorization URL for gateway authentication.
         *
         * @param url The authorization URL.
         * @return The builder instance.
         */
        fun gatewayAuthorizationUrl(url: String) = apply {
            this._authorizationUrl = url
        }

        /**
         * Sets the payment page URL.
         *
         * @param url The payment page URL.
         * @return The builder instance.
         */
        fun payPageUrl(url: String) = apply {
            this._paymentUrl = url
        }

        /**
         * Sets the Google Pay configuration for the payment request.
         *
         * @param googlePayConfig The Google Pay configuration.
         * @return The builder instance.
         */
        fun setGooglePayConfig(googlePayConfig: GooglePayConfig) = apply {
            this._googlePayConfig = googlePayConfig
        }

        /**
         * Builds the `PaymentsRequest` instance.
         *
         * @return An instance of `PaymentsRequest`.
         * @throws IllegalStateException if required fields are not initialized.
         */
        fun build(): PaymentsRequest {
            check(this::_authorizationUrl.isInitialized) { "Gateway url should not be null" }
            check(this::_paymentUrl.isInitialized) { "Pay page url should not be null" }
            return PaymentsRequest(
                authorizationUrl = _authorizationUrl,
                paymentUrl = _paymentUrl,
                googlePayConfig = _googlePayConfig
            )
        }
    }

    private fun toBundle() = bundleOf(EXTRA_ARGS to this)

    /**
     * Converts the request to an Intent for starting the payment activity.
     *
     * @param context The context to create the intent.
     * @return The intent for launching the payment page.
     */
    fun toIntent(context: Context) = Intent(
        context,
        PaymentsActivity::class.java
    ).apply {
        putExtra(
            EXTRA_INTENT,
            toBundle()
        )
    }

    companion object {
        /**
         * Creates a new builder instance for `PaymentsRequest`.
         *
         * @return A new `PaymentsRequest.Builder` instance.
         */
        fun builder() = Builder()
        private const val EXTRA_ARGS = "payments_request_args"
        private const val EXTRA_INTENT = "payments_request_args_intent"

        /**
         * Retrieves a `PaymentsRequest` from an intent.
         *
         * @param intent The intent containing the `PaymentsRequest`.
         * @return The `PaymentsRequest` if available, or `null`.
         */
        fun fromIntent(intent: Intent): PaymentsRequest? {
            val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
            return inputIntent?.getParcelable(EXTRA_ARGS)
        }
    }
}