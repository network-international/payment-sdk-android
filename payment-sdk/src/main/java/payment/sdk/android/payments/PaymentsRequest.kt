package payment.sdk.android.payments

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.interactor.ClickToPayConfig
import payment.sdk.android.googlepay.GooglePayConfig
import payment.sdk.android.payments.model.OrderItem

/**
 * `UnifiedPaymentPageRequest` class represents the request for launching the payment page.
 *
 * Use the `Builder` pattern to set necessary parameters and create an instance.
 *
 * Usage:
 * ```
 * val request = UnifiedPaymentPageRequest.builder()
 *     .gatewayAuthorizationUrl(authUrl)
 *     .payPageUrl(payPageUrl)
 *     .build()
 * ```
 *
 * @property authorizationUrl The authorization URL for gateway authentication.
 * @property paymentUrl The URL for the payment page.
 * @property googlePayConfig Optional configuration for Google Pay.
 * @property clickToPayConfig Optional configuration for Click to Pay.
 */
@Parcelize
class UnifiedPaymentPageRequest private constructor(
    val authorizationUrl: String,
    val paymentUrl: String,
    val googlePayConfig: GooglePayConfig?,
    val clickToPayConfig: ClickToPayConfig?,
    val samsungPayConfig: SamsungPayConfig?,
    val savedCards: List<SavedCard> = emptyList(),
    val orderItems: List<OrderItem> = emptyList()
) : Parcelable {

    /**
     * Builder class for []UnifiedPaymentPageRequest].
     */
    class Builder {
        private lateinit var _authorizationUrl: String
        private lateinit var _paymentUrl: String
        private var _googlePayConfig: GooglePayConfig? = null
        private var _clickToPayConfig: ClickToPayConfig? = null
        private var _samsungPayConfig: SamsungPayConfig? = null
        private var _savedCards: List<SavedCard> = emptyList()
        private var _orderItems: List<OrderItem> = emptyList()

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
         * Sets the Click to Pay configuration for the payment request.
         *
         * @param clickToPayConfig The Click to Pay configuration.
         * @return The builder instance.
         */
        fun setClickToPayConfig(clickToPayConfig: ClickToPayConfig) = apply {
            this._clickToPayConfig = clickToPayConfig
        }

        /**
         * Sets the Samsung Pay configuration for the payment request.
         * When provided, Samsung Pay is handled entirely within the payment page.
         *
         * @param samsungPayConfig The Samsung Pay configuration.
         * @return The builder instance.
         */
        fun setSamsungPayConfig(samsungPayConfig: SamsungPayConfig) = apply {
            this._samsungPayConfig = samsungPayConfig
        }

        fun setSavedCards(savedCards: List<SavedCard>) = apply {
            this._savedCards = savedCards
        }

        fun setOrderItems(orderItems: List<OrderItem>) = apply {
            this._orderItems = orderItems
        }

        /**
         * Builds the `UnifiedPaymentPageRequest` instance.
         *
         * @return An instance of `UnifiedPaymentPageRequest`.
         * @throws IllegalStateException if required fields are not initialized.
         */
        fun build(): UnifiedPaymentPageRequest {
            check(this::_authorizationUrl.isInitialized) { "Gateway url should not be null" }
            check(this::_paymentUrl.isInitialized) { "Pay page url should not be null" }
            return UnifiedPaymentPageRequest(
                authorizationUrl = _authorizationUrl,
                paymentUrl = _paymentUrl,
                googlePayConfig = _googlePayConfig,
                clickToPayConfig = _clickToPayConfig,
                samsungPayConfig = _samsungPayConfig,
                savedCards = _savedCards,
                orderItems = _orderItems
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
        UnifiedPaymentPageActivity::class.java
    ).apply {
        putExtra(
            EXTRA_INTENT,
            toBundle()
        )
    }

    companion object {
        /**
         * Creates a new builder instance for `UnifiedPaymentPageRequest`.
         *
         * @return A new `UnifiedPaymentPageRequest.Builder` instance.
         */
        fun builder() = Builder()
        private const val EXTRA_ARGS = "payments_request_args"
        private const val EXTRA_INTENT = "payments_request_args_intent"

        /**
         * Retrieves a `UnifiedPaymentPageRequest` from an intent.
         *
         * @param intent The intent containing the `UnifiedPaymentPageRequest`.
         * @return The `UnifiedPaymentPageRequest` if available, or `null`.
         */
        fun fromIntent(intent: Intent): UnifiedPaymentPageRequest? {
            val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
            return inputIntent?.getParcelable(EXTRA_ARGS)
        }
    }
}