package payment.sdk.android.googlepay

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
 * Launches Google Pay without opening the unified payment page.
 *
 * The launcher authorizes the order, opens the Google Pay sheet, posts the token to the
 * gateway accept URL, and reports the payment state.
 *
 * Usage:
 * ```
 * private val googlePayLauncher = GooglePayLauncher(this) { result ->
 *     when (result) {
 *         GooglePayLauncher.Result.Success -> …
 *         GooglePayLauncher.Result.Authorised -> …
 *         GooglePayLauncher.Result.Captured -> …
 *         GooglePayLauncher.Result.PostAuthReview -> …
 *         is GooglePayLauncher.Result.Failed -> …
 *         GooglePayLauncher.Result.Cancelled -> …
 *     }
 * }
 *
 * googlePayLauncher.launch(
 *     GooglePayLauncher.Config(
 *         gatewayAuthorizationUrl = authUrl,
 *         payPageUrl = payPageUrl,
 *         googlePayConfig = GooglePayConfig(
 *             environment = GooglePayConfig.Environment.Test,
 *             merchantGatewayId = "…"
 *         )
 *     )
 * )
 * ```
 */
class GooglePayLauncher(
    private val activityResultLauncher: ActivityResultLauncher<Config>
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: ResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            GooglePayLauncherContract(),
            resultCallback::onResult
        )
    )

    @Throws(IllegalArgumentException::class)
    fun launch(config: Config) {
        activityResultLauncher.launch(config)
    }

    sealed class Result : Parcelable {
        @Parcelize
        data object Success : Result()

        @Parcelize
        data object Authorised : Result()

        @Parcelize
        data object Captured : Result()

        @Parcelize
        data object PostAuthReview : Result()

        @Parcelize
        data class Failed(val error: String) : Result()

        @Parcelize
        data object Cancelled : Result()
    }

    @Parcelize
    data class Config(
        val gatewayAuthorizationUrl: String,
        val payPageUrl: String,
        val googlePayConfig: GooglePayConfig
    ) : Parcelable {

        private fun toBundle() = bundleOf(EXTRA_ARGS to this)

        fun toIntent(context: Context) = Intent(
            context,
            GooglePayActivity::class.java
        ).apply {
            putExtra(EXTRA_INTENT, toBundle())
        }

        companion object {
            private const val EXTRA_ARGS = "google_pay_args"
            private const val EXTRA_INTENT = "google_pay_args_intent"

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

fun interface ResultCallback {
    fun onResult(result: GooglePayLauncher.Result)
}

@Composable
fun rememberGooglePayLauncher(
    resultCallback: ResultCallback
): GooglePayLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        GooglePayLauncherContract(),
        resultCallback::onResult
    )
    return remember {
        GooglePayLauncher(activityResultLauncher = activityResultLauncher)
    }
}
