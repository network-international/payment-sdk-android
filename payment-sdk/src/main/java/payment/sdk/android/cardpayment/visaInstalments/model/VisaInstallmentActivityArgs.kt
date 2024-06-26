package payment.sdk.android.cardpayment.visaInstalments.model

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import payment.sdk.android.cardpayment.savedCard.SavedCardDto
import payment.sdk.android.cardpayment.visaInstalments.VisaInstallmentsActivity
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.VisaPlans
import kotlin.jvm.Throws

@Parcelize
class VisaInstalmentActivityArgs(
    val orderUrl: String,
    val paymentCookie: String,
    val savedCard: SavedCardDto?,
    val newCard: NewCardDto?,
    val savedCardUrl: String?,
    val paymentUrl: String?,
    val payPageUrl: String,
    val instalmentPlan: List<InstallmentPlan>,
    val cvv: String? = null,
    val accessToken: String
) : Parcelable {

    private fun toBundle() = bundleOf(EXTRA_ARGS to this)

    fun toIntent(context: Context) = Intent(
        context,
        VisaInstallmentsActivity::class.java
    ).apply {
        putExtra(
            EXTRA_INTENT,
            toBundle()
        )
    }

    internal companion object {
        private const val EXTRA_ARGS = "visa_instalment_args"
        private const val EXTRA_INTENT = "visa_instalment_args_intent"

        fun fromIntent(intent: Intent): VisaInstalmentActivityArgs? {
            val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
            return inputIntent?.getParcelable(EXTRA_ARGS)
        }

        @Throws(IllegalArgumentException::class)
        fun getArgs(
            paymentCookie: String,
            accessToken: String,
            paymentUrl: String?,
            savedCardUrl: String?,
            payPageUrl: String,
            visaPlans: VisaPlans,
            savedCard: SavedCardDto?,
            newCard: NewCardDto?,
            orderUrl: String,
            orderAmount: OrderAmount,
            cvv: String? = null,
        ): VisaInstalmentActivityArgs {
            return VisaInstalmentActivityArgs(
                paymentCookie = paymentCookie,
                savedCard = savedCard,
                newCard = newCard,
                savedCardUrl = savedCardUrl,
                paymentUrl = paymentUrl,
                payPageUrl = payPageUrl,
                instalmentPlan = InstallmentPlan.fromVisaPlans(visaPlans, orderAmount),
                orderUrl = orderUrl,
                cvv = cvv,
                accessToken = accessToken
            )
        }
    }
}

@Parcelize
data class NewCardDto(
    val cardNumber: String,
    val expiry: String,
    val cvv: String,
    val customerName: String
) : Parcelable