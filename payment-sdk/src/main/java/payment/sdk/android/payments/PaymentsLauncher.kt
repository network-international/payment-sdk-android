package payment.sdk.android.payments

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

class CardPaymentsLauncher(
    private val activityResultLauncher: ActivityResultLauncher<CardPaymentsIntent>,
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: CardPaymentsResultCallback,
    ) : this(
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

    fun launch(paymentsRequest: PaymentsRequest) {
        activityResultLauncher.launch(
            CardPaymentsIntent(
                authorizationUrl = paymentsRequest.authorizationUrl,
                paymentUrl = paymentsRequest.paymentUrl
            )
        )
    }

    @Parcelize
    class CardPaymentsIntent(
        val authorizationUrl: String,
        val paymentUrl: String,
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