package payment.sdk.android.knet

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

/**
 * Launcher for KNET, the Kuwait domestic debit scheme.
 *
 * The flow:
 * 1. Re-read the order and take the payment's current `payment:knet` link.
 * 2. POST `{}` to it to obtain the hosted `paymentUrl` on KNET's site.
 * 3. Load that page in a WebView, where the payer pays or cancels.
 * 4. Once the WebView leaves KNET's site, read the outcome from the order's `payment.state` —
 *    never from the redirect.
 */
class KnetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<Config>
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: KnetResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            KnetLauncherContract(),
            resultCallback::onResult
        )
    )

    fun launch(config: Config) {
        activityResultLauncher.launch(config)
    }

    sealed class Result : Parcelable {
        @Parcelize data object Success : Result()
        /** Terminal, but not a decline — fulfilment must be held until the review clears. */
        @Parcelize data object PostAuthReview : Result()
        @Parcelize data class Failed(val error: String) : Result()
        /**
         * KNET never started — no `payment:knet` link, the gateway refused the initiation, or KNET's
         * page never loaded. Nothing was charged; the payer goes back to the page with KNET marked
         * unavailable and every other method still open.
         */
        @Parcelize data class Unavailable(val error: String) : Result()
        /**
         * The payer left before KNET settled anything. The order is still payable — the gateway
         * accepts a fresh KNET initiation on it — so they go back to the page to try again.
         */
        @Parcelize data object Canceled : Result()
        /** Cancelled on KNET's own page. Also returns the payer to the page on the same order. */
        @Parcelize data object CanceledOnProvider : Result()
        @Parcelize data object InvalidRequest : Result()
    }

    @Parcelize
    data class Config(
        /** The payment's `payment:knet` link when the page was built; used if the order cannot be re-read. */
        val knetUrl: String,
        /** Order self-link: re-read before each attempt and polled for the result. */
        val orderUrl: String,
        /** Bearer access token. */
        val accessToken: String
    ) : Parcelable {

        private fun toBundle() = bundleOf(EXTRA_ARGS to this)

        fun toIntent(context: Context): Intent =
            Intent(context, KnetActivity::class.java).apply {
                putExtra(EXTRA_INTENT, toBundle())
            }

        companion object {
            private const val EXTRA_ARGS = "knet_args"
            private const val EXTRA_INTENT = "knet_args_intent"

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

fun interface KnetResultCallback {
    fun onResult(result: KnetLauncher.Result)
}

@Composable
fun rememberKnetLauncher(resultCallback: KnetResultCallback): KnetLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        KnetLauncherContract(),
        resultCallback::onResult
    )
    return remember { KnetLauncher(activityResultLauncher) }
}
