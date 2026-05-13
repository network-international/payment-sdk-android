package payment.sdk.android.qpay

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
 * Launcher for QPay (Qatar Central Bank gateway via Doha Bank EZConnect).
 *
 * The flow:
 * 1. Caller provides the order's `qpayLink`, the `payPageUrl` (used to derive the whitelisted
 *    Origin), the order self-link (for status refetch), and an `accessToken`.
 * 2. SDK calls `qpayLink` to obtain the QCB form fields.
 * 3. SDK loads an auto-submit HTML form in a WebView using the PayPageV2 origin as base URL —
 *    this makes the cross-origin POST to QCB carry the whitelisted Origin header.
 * 4. After payment, QCB redirects to the backend `/qpay/accept` callback. We watch for it,
 *    refetch the order, and report Success/Failed based on `payment.state`.
 */
class QPayLauncher(
    private val activityResultLauncher: ActivityResultLauncher<Config>
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: ResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            QPayLauncherContract(),
            resultCallback::onResult
        )
    )

    @Throws(IllegalArgumentException::class)
    fun launch(config: Config) {
        activityResultLauncher.launch(config)
    }

    sealed class Result : Parcelable {
        @Parcelize data object Success : Result()
        @Parcelize data class Failed(val error: String) : Result()
        @Parcelize data object Canceled : Result()
        @Parcelize data object InvalidRequest : Result()
    }

    @Parcelize
    data class Config(
        /** Backend QPay endpoint (`payment:qpay` link from order). */
        val qpayUrl: String,
        /** Order's payPageUrl — origin (scheme+host) becomes the WebView baseURL. */
        val payPageUrl: String,
        /** Order self-link, used to refetch order state after the callback. */
        val orderUrl: String,
        /** Bearer access token. */
        val accessToken: String,
        /** Currency code from the order. QPay only supports QAR. */
        val currencyCode: String
    ) : Parcelable {

        private fun toBundle() = bundleOf(EXTRA_ARGS to this)

        fun toIntent(context: Context): Intent =
            Intent(context, QPayActivity::class.java).apply {
                putExtra(EXTRA_INTENT, toBundle())
            }

        companion object {
            private const val EXTRA_ARGS = "qpay_args"
            private const val EXTRA_INTENT = "qpay_args_intent"

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

fun interface QPayResultCallback {
    fun onResult(result: QPayLauncher.Result)
}

@Composable
fun rememberQPayLauncher(resultCallback: QPayResultCallback): QPayLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        QPayLauncherContract(),
        resultCallback::onResult
    )
    return remember { QPayLauncher(activityResultLauncher) }
}

// Reuse type alias for parity with ClickToPay's ResultCallback fun-interface naming.
typealias ResultCallback = QPayResultCallback
