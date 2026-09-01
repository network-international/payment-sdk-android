package payment.sdk.android.bnpl

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
import payment.sdk.android.core.BnplProvider
import kotlin.jvm.Throws

/**
 * Launcher for the buy-now-pay-later providers — Tamara and Tabby, which share this flow exactly.
 *
 * The flow:
 * 1. Caller provides the order's provider endpoint, the order self-link (for the status poll), an
 *    `accessToken` and the three return URLs.
 * 2. The SDK POSTs the endpoint to obtain the provider's hosted checkout URL and its own reference.
 * 3. That URL is loaded in a WebView where the payer approves the instalment plan.
 * 4. The provider redirects to whichever return URL matches the outcome. The SDK intercepts it (it
 *    never loads), POSTs the reference to `/accept` so the backend can finalise the payment, then
 *    polls the order and reports the outcome from `payment.state`.
 */
class BnplLauncher(
    private val activityResultLauncher: ActivityResultLauncher<Config>
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: BnplResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            BnplLauncherContract(),
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
        /**
         * The checkout never started — the gateway or the provider refused to open one. Nothing was
         * charged and the order is untouched, so this ends the *option*, not the payment: the payer
         * goes back to the page with the row marked unavailable and every other method still open.
         * Reported separately from [Failed] because a provider that cannot be reached must not cost
         * the merchant a sale the payer was willing to complete by card.
         */
        @Parcelize data class Unavailable(val provider: BnplProvider, val error: String) : Result()
        /**
         * Backed out before the provider recorded anything against the payment. The order is
         * untouched and still payable, so the payer can go back and choose another method.
         */
        @Parcelize data object Canceled : Result()
        /**
         * Cancelled on the provider's own hosted page. The payer backing out is not a payment
         * outcome, so the SDK returns them to the payment page instead of ending the payment.
         */
        @Parcelize data object CanceledOnProvider : Result()
        @Parcelize data object InvalidRequest : Result()
    }

    @Parcelize
    data class Config(
        val provider: BnplProvider,
        /** Checkout initiation endpoint for this provider. */
        val checkoutUrl: String,
        /** Endpoint that hands the provider's outcome back to the backend. */
        val acceptUrl: String,
        /** Order self-link, polled for the payment state after the return leg. */
        val orderUrl: String,
        /** Bearer access token. */
        val accessToken: String,
        /** Where the provider sends the payer when the checkout is approved. */
        val successUrl: String,
        /** Where the provider sends the payer when they abandon the checkout. */
        val cancelUrl: String,
        /** Where the provider sends the payer when the checkout is declined or errors. */
        val failureUrl: String
    ) : Parcelable {

        private fun toBundle() = bundleOf(EXTRA_ARGS to this)

        fun toIntent(context: Context): Intent =
            Intent(context, BnplActivity::class.java).apply {
                putExtra(EXTRA_INTENT, toBundle())
            }

        companion object {
            private const val EXTRA_ARGS = "bnpl_args"
            private const val EXTRA_INTENT = "bnpl_args_intent"

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

fun interface BnplResultCallback {
    fun onResult(result: BnplLauncher.Result)
}

@Composable
fun rememberBnplLauncher(resultCallback: BnplResultCallback): BnplLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        BnplLauncherContract(),
        resultCallback::onResult
    )
    return remember { BnplLauncher(activityResultLauncher) }
}
