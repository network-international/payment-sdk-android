package payment.sdk.android.cardpayment.savedCard

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import payment.sdk.android.core.Order
import kotlin.jvm.Throws

@Parcelize
data class SavedCardActivityArgs(
    val savedCardUrl: String,
    val authUrl: String,
    val paymentUrl: String,
    val savedCard: SavedCardDto,
    val amount: Double,
    val currency: String,
    val cvv: String?,
    val selfUrl: String
) : Parcelable {
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

    internal companion object {
        private const val EXTRA_ARGS = "saved_card_args"
        private const val EXTRA_INTENT = "saved_card_args_intent"

        fun fromIntent(intent: Intent): SavedCardActivityArgs? {
            val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
            return inputIntent?.getParcelable(EXTRA_ARGS)
        }

        @Throws(IllegalArgumentException::class)
        fun getArgs(order: Order, cvv: String? = null): SavedCardActivityArgs {
            val savedCard = requireNotNull(order.savedCard) {
                "Saved card info not found, saved card token needs to be passed in order request"
            }
            return SavedCardActivityArgs(
                savedCardUrl = requireNotNull(order.embedded?.payment?.first()?.links?.savedCard?.href) {
                    "Saved Card URL not found"
                },
                authUrl = requireNotNull(order.links?.paymentAuthorizationUrl?.href) { "Auth URL not found " },
                paymentUrl = requireNotNull(order.links?.paymentUrl?.href) { "Payment URL not found" },
                savedCard = SavedCardDto.from(savedCard),
                amount = requireNotNull(order.amount?.value) { "Amount value not found" },
                currency = requireNotNull(order.amount?.currencyCode) { "currency code not found" },
                cvv = cvv,
                selfUrl = requireNotNull(order.embedded?.payment?.firstOrNull()?.links?.selfLink?.href) {
                    "Self URL link not found"
                }
            )
        }
    }
}