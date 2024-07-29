package payment.sdk.android.cardpayment.aaniPay.model

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import payment.sdk.android.cardpayment.aaniPay.AaniPayActivity
import payment.sdk.android.core.Order

@Parcelize
class AaniPayActivityArgs(
    val amount: Double,
    val anniPaymentLink: String,
    val currencyCode: String,
    val authUrl: String,
    val payPageUrl: String
) : Parcelable {

    private fun toBundle() = bundleOf(EXTRA_ARGS to this)

    fun toIntent(context: Context) = Intent(
        context,
        AaniPayActivity::class.java
    ).apply {
        putExtra(
            EXTRA_INTENT,
            toBundle()
        )
    }

    internal companion object {
        private const val EXTRA_ARGS = "aani_pay_args"
        private const val EXTRA_INTENT = "aani_pay_args_intent"

        fun fromIntent(intent: Intent): AaniPayActivityArgs? {
            val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
            return inputIntent?.getParcelable(EXTRA_ARGS)
        }

        @Throws(IllegalArgumentException::class)
        fun getArgs(
            order: Order,
        ): AaniPayActivityArgs {
            return AaniPayActivityArgs(
                amount = requireNotNull(order.amount?.value) {
                    "Order Amount Not found"
                },
                currencyCode = requireNotNull(order.amount?.currencyCode) {
                    "Currency Code not found"
                },
                anniPaymentLink = requireNotNull(order.embedded?.payment?.first()?.links?.aaniPayment?.href) {
                    "Aani Payment Link not found"
                },
                payPageUrl = requireNotNull(order.links?.paymentUrl?.href) { "Payment URL not found" },
                authUrl = requireNotNull(order.links?.paymentAuthorizationUrl?.href) { "Auth URL not found " }
            )
        }
    }
}