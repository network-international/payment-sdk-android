package payment.sdk.android.payments

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import payment.sdk.android.googlepay.GooglePayConfig

@Parcelize
class PaymentsRequest private constructor(
    val authorizationUrl: String,
    val paymentUrl: String,
    val language: String,
    val googlePayConfig: GooglePayConfig?
) : Parcelable {

    class Builder {
        private lateinit var _authorizationUrl: String
        private lateinit var _paymentUrl: String
        private var _language: String = "en"
        private var _googlePayConfig: GooglePayConfig? = null

        fun gatewayAuthorizationUrl(url: String) = apply {
            this._authorizationUrl = url
        }

        fun payPageUrl(url: String) = apply {
            this._paymentUrl = url
        }

        fun setLanguageCode(languageCode: String) = apply {
            this._language = languageCode
        }

        fun setGooglePayConfig(googlePayConfig: GooglePayConfig) = apply {
            this._googlePayConfig = googlePayConfig
        }

        fun build(): PaymentsRequest {
            check(this::_authorizationUrl.isInitialized) { "Gateway url should not be null" }
            check(this::_paymentUrl.isInitialized) { "Pay page url should not be null" }
            return PaymentsRequest(_authorizationUrl, _paymentUrl, _language, _googlePayConfig)
        }
    }

    private fun toBundle() = bundleOf(EXTRA_ARGS to this)

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
        fun builder() = Builder()
        private const val EXTRA_ARGS = "payments_request_args"
        private const val EXTRA_INTENT = "payments_request_args_intent"

        fun fromIntent(intent: Intent): PaymentsRequest? {
            val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
            return inputIntent?.getParcelable(EXTRA_ARGS)
        }
    }
}