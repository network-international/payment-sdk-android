package payment.sdk.android.benefit

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
import kotlin.jvm.Throws

/**
 * Launcher for Benefit, the Bahrain domestic debit scheme.
 *
 * The flow:
 * 1. Caller provides the order's Benefit endpoint, the order self-link (for the status poll) and an
 *    `accessToken`.
 * 2. The SDK POSTs the Benefit endpoint to obtain the hosted `paymentUrl`.
 * 3. That URL is loaded in a WebView where the payer authenticates with their bank.
 * 4. Benefit posts the result back to the gateway's `.../benefit/Response/accept` (or
 *    `/Error/accept`) callback. The SDK watches for it, then polls the order and reports the
 *    outcome from `payment.state`.
 */
class BenefitLauncher(
    private val activityResultLauncher: ActivityResultLauncher<Config>
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: BenefitResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            BenefitLauncherContract(),
            resultCallback::onResult
        )
    )

    @Throws(IllegalArgumentException::class)
    fun launch(config: Config) {
        activityResultLauncher.launch(config)
    }

    sealed class Result : Parcelable {
        @Parcelize data object Success : Result()
        /** Terminal, but not a decline — fulfilment must be held until the review clears. */
        @Parcelize data object PostAuthReview : Result()
        @Parcelize data class Failed(val error: String) : Result()
        @Parcelize data object Canceled : Result()
        @Parcelize data object InvalidRequest : Result()
    }

    @Parcelize
    data class Config(
        /** Benefit initiation endpoint, derived from the payment self-link. */
        val benefitUrl: String,
        /** Order self-link, polled for the payment state after the callback. */
        val orderUrl: String,
        /** Bearer access token. */
        val accessToken: String,
        /** Currency code from the order. Benefit only supports BHD. */
        val currencyCode: String
    ) : Parcelable {

        private fun toBundle() = bundleOf(EXTRA_ARGS to this)

        fun toIntent(context: Context): Intent =
            Intent(context, BenefitActivity::class.java).apply {
                putExtra(EXTRA_INTENT, toBundle())
            }

        companion object {
            private const val EXTRA_ARGS = "benefit_args"
            private const val EXTRA_INTENT = "benefit_args_intent"

            @Suppress("DEPRECATION")
            fun fromIntent(intent: Intent): Config? {
                val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
                return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    inputIntent?.getParcelable(EXTRA_ARGS, Config::class.java)
                } else {
                    inputIntent?.getParcelable(EXTRA_ARGS)
                }
            }
        }
    }
}

fun interface BenefitResultCallback {
    fun onResult(result: BenefitLauncher.Result)
}

@Composable
fun rememberBenefitLauncher(resultCallback: BenefitResultCallback): BenefitLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        BenefitLauncherContract(),
        resultCallback::onResult
    )
    return remember { BenefitLauncher(activityResultLauncher) }
}
