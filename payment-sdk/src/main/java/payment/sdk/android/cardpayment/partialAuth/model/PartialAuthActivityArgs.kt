package payment.sdk.android.cardpayment.partialAuth.model

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import payment.sdk.android.cardpayment.partialAuth.PartialAuthActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent

@Parcelize
class PartialAuthActivityArgs(
    val partialAmount: Double,
    val amount: Double,
    val currency: String,
    val acceptUrl: String,
    val declineUrl: String,
    val issuingOrg: String?,
    val paymentCookie: String
) : Parcelable {
    private fun toBundle() = bundleOf(EXTRA_ARGS to this)

    fun toIntent(context: Context) = Intent(
        context,
        PartialAuthActivity::class.java
    ).apply {
        putExtra(
            EXTRA_INTENT,
            toBundle()
        )
    }

    internal companion object {
        private const val EXTRA_ARGS = "partial_auth_args"
        private const val EXTRA_INTENT = "partial_auth_intent"

        fun fromIntent(intent: Intent): PartialAuthActivityArgs? {
            val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
            return inputIntent?.getParcelable(EXTRA_ARGS)
        }

        @Throws(IllegalArgumentException::class)
        fun getArgs(partialAuthIntent: PartialAuthIntent?): PartialAuthActivityArgs {
            return PartialAuthActivityArgs(
                partialAmount = requireNotNull(partialAuthIntent?.partialAmount) {
                    "ThreeDSChallengeResponse Partial amount not found"
                },
                amount = requireNotNull(partialAuthIntent?.amount) {
                    "ThreeDSChallengeResponse Amount not found"
                },
                acceptUrl = requireNotNull(partialAuthIntent?.acceptUrl) {
                    "ThreeDSChallengeResponse acceptUrl not found"
                },
                declineUrl = requireNotNull(partialAuthIntent?.declineUrl) {
                    "ThreeDSChallengeResponse declineUrl not found"
                },
                issuingOrg = partialAuthIntent?.issuingOrg,
                currency = requireNotNull(partialAuthIntent?.currency) {
                    "ThreeDSChallengeResponse currencyCode not found"
                },
                paymentCookie = requireNotNull(partialAuthIntent?.paymentCookie) {
                    "ThreeDSChallengeResponse payment cookie not found"
                }
            )
        }
    }
}