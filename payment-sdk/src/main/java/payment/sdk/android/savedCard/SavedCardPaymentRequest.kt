package payment.sdk.android.savedCard

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
class SavedCardPaymentRequest private constructor(
    val authorizationUrl: String,
    val paymentUrl: String,
    val language: String,
    val cvv: String?
) : Parcelable {
    class Builder {
        private lateinit var _authorizationUrl: String
        private lateinit var _paymentUrl: String
        private var _language: String = "en"
        private var _cvv: String? = null

        fun gatewayAuthorizationUrl(url: String) = apply {
            this._authorizationUrl = url
        }

        fun payPageUrl(url: String) = apply {
            this._paymentUrl = url
        }

        fun setLanguageCode(languageCode: String) = apply {
            this._language = languageCode
        }

        fun setCvv(cvv: String) = apply {
            this._cvv = cvv
        }

        fun build(): SavedCardPaymentRequest {
            check(this::_authorizationUrl.isInitialized || _authorizationUrl.isEmpty()) { "Gateway url should not be null" }
            check(this::_paymentUrl.isInitialized || _paymentUrl.isEmpty()) { "Pay page url should not be null" }
            return SavedCardPaymentRequest(_authorizationUrl, _paymentUrl, _language, _cvv)
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
        fun builder() = Builder()
        private const val EXTRA_ARGS = "saved_card_payments_request_args"
        private const val EXTRA_INTENT = "saved_card_payments_request_args_intent"

        fun fromIntent(intent: Intent): SavedCardPaymentRequest? {
            val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
            return inputIntent?.getParcelable(EXTRA_ARGS)
        }
    }
}