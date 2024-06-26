package payment.sdk.android.cardpayment.aaniPay.model

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import kotlinx.parcelize.Parcelize
import payment.sdk.android.cardpayment.aaniPay.AaniPayActivity
import payment.sdk.android.core.Order
import payment.sdk.android.core.OrderAmount
import java.util.Locale

@Parcelize
class AaniPayActivityArgs(
    val amount: String
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
            val isLTR =
                TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR
            return AaniPayActivityArgs(
                amount = OrderAmount(
                    orderValue = order.amount?.value ?: 0.0,
                    currencyCode = order.amount?.currencyCode ?: ""
                ).formattedCurrencyString2Decimal(isLTR)
            )
        }
    }
}