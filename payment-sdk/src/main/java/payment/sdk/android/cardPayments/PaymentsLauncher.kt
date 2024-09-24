package payment.sdk.android.cardPayments

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import payment.sdk.android.core.Order

class CardPaymentsLauncher(
    private val activityResultLauncher: ActivityResultLauncher<CardPaymentsIntent>,
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: CardPaymentsResultCallback,
    ): this(
        activityResultLauncher = activity.registerForActivityResult(
            PaymentsLauncherContract(),
            resultCallback::onResult
        )
    )

    sealed class Result : Parcelable {
        @Parcelize
        data object Authorised : Result()

        @Parcelize
        data object Success : Result()

        @Parcelize
        data object PostAuthReview : Result()

        @Parcelize
        data object PartialAuthDeclined : Result()

        @Parcelize
        data object PartialAuthDeclineFailed : Result()

        @Parcelize
        data object PartiallyAuthorised : Result()

        @Parcelize
        data class Failed(val error: String) : Result()

        @Parcelize
        data object Cancelled : Result()
    }

    fun launch(cardPaymentsIntent: CardPaymentsIntent) {
        activityResultLauncher.launch(cardPaymentsIntent)
    }

    @Parcelize
    class CardPaymentsIntent(
        val selfUrl: String,
        val authUrl: String,
        val payPageUrl: String,
        val outletId: String,
        val cardPaymentUrl: String,
        val amount: Double,
        val currencyCode: String,
        val googlePayUrl: String?,
        val googlePayConfigUrl: String?,
        val allowedWallets: List<String>,
        val allowedCards: List<String>,
        val language: String
    ) : Parcelable {
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
            private const val EXTRA_ARGS = "aani_pay_args"
            private const val EXTRA_INTENT = "aani_pay_args_intent"

            fun fromIntent(intent: Intent): CardPaymentsIntent? {
                val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
                return inputIntent?.getParcelable(EXTRA_ARGS)
            }

            @Throws(IllegalArgumentException::class)
            fun create(
                order: Order,
            ): CardPaymentsIntent {
                val paymentLinks = order.embedded?.payment?.first()?.links
                return CardPaymentsIntent(
                    selfUrl = requireNotNull(paymentLinks?.selfLink?.href),
                    amount = requireNotNull(order.amount?.value) {
                        "Order Amount Not found"
                    },
                    authUrl = requireNotNull(order.links?.paymentAuthorizationUrl?.href) {
                        "Currency Code not found"
                    },
                    outletId = requireNotNull(order.outletId) {
                        "Outlet ID not found"
                    },
                    payPageUrl = requireNotNull(order.links?.paymentUrl?.href) { "Payment URL not found" },
                    currencyCode = requireNotNull(order.amount?.currencyCode) {
                        "Currency Code not found"
                    },
                    googlePayUrl = paymentLinks?.googlePayLink?.href,
                    cardPaymentUrl = requireNotNull(paymentLinks?.card?.href) {
                        "Card Payment URL not found"
                    },
                    allowedCards = order.paymentMethods?.card ?: emptyList(),
                    allowedWallets = order.paymentMethods?.wallet?.asList() ?: emptyList(),
                    googlePayConfigUrl = paymentLinks?.googlePayConfigLink?.href,
                    language = order.language
                )
            }
        }
    }
}

fun interface CardPaymentsResultCallback {
    fun onResult(result: CardPaymentsLauncher.Result)
}

@Composable
fun rememberGooglePayLauncher(
    resultCallback: CardPaymentsResultCallback
): CardPaymentsLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        PaymentsLauncherContract(),
        resultCallback::onResult
    )
    return remember {
        CardPaymentsLauncher(
            activityResultLauncher = activityResultLauncher,
        )
    }
}