package payment.sdk.android.clicktopay

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
import payment.sdk.android.core.interactor.ClickToPayConfig
import kotlin.jvm.Throws

/**
 * Launcher for Click to Pay (Unified Click to Pay) payment flow.
 *
 * Usage:
 * ```kotlin
 * class CheckoutActivity : ComponentActivity() {
 *     private lateinit var clickToPayLauncher: ClickToPayLauncher
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         clickToPayLauncher = ClickToPayLauncher(this) { result ->
 *             when (result) {
 *                 is ClickToPayLauncher.Result.Success -> // Handle success
 *                 is ClickToPayLauncher.Result.Failed -> // Handle failure
 *                 is ClickToPayLauncher.Result.Canceled -> // Handle cancellation
 *             }
 *         }
 *     }
 *
 *     fun startClickToPay() {
 *         clickToPayLauncher.launch(
 *             ClickToPayLauncher.Config(
 *                 clickToPayConfig = clickToPayConfig,
 *                 clickToPayUrl = orderUrl,
 *                 amount = amount,
 *                 currencyCode = "USD",
 *                 accessToken = token,
 *                 paymentCookie = cookie
 *             )
 *         )
 *     }
 * }
 * ```
 */
class ClickToPayLauncher(
    private val activityResultLauncher: ActivityResultLauncher<Config>
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: ResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            ClickToPayLauncherContract(),
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
        data class Failed(
            val error: String
        ) : Result()

        @Parcelize
        data object Canceled : Result()

        @Parcelize
        data class Requires3DS(
            val acsUrl: String,
            val acsPaReq: String,
            val acsMd: String
        ) : Result()

        @Parcelize
        data class Requires3DSTwo(
            val threeDSMethodUrl: String?,
            val threeDSServerTransId: String?,
            val directoryServerId: String?,
            val threeDSMessageVersion: String?,
            val acsUrl: String?,
            val threeDSTwoAuthenticationURL: String?,
            val threeDSTwoChallengeResponseURL: String?,
            val outletRef: String?,
            val orderRef: String?,
            val paymentRef: String?,
            val threeDSMethodData: String?,
            val threeDSMethodNotificationURL: String?,
            val paymentCookie: String,
            val orderUrl: String?
        ) : Result()
    }

    @Parcelize
    data class Config(
        val clickToPayConfig: ClickToPayConfig,
        val clickToPayUrl: String,
        val amount: Double,
        val currencyCode: String,
        val accessToken: String,
        val paymentCookie: String,
        val orderReference: String? = null,
        val merchantName: String? = null,
        /** Outlet ID for building unified-click-to-pay URL */
        val outletId: String? = null,
        /** Order ID for building unified-click-to-pay URL */
        val orderId: String? = null,
        /** Payment reference for building unified-click-to-pay URL */
        val paymentRef: String? = null,
        /** Pay page URL for two-stage origin loading */
        val payPageUrl: String? = null,
        /** Order URL for polling after payment */
        val orderUrl: String? = null,
        /** Pre-populated email from email entry screen */
        val userEmail: String? = null,
        /** When true, skip SDK init and show OTP page directly (for testing) */
        val testOtpMode: Boolean = false,
        val locale: String = "en"
    ) : Parcelable {

        /**
         * Build the unified-click-to-pay API URL.
         * Uses the pay page host (not the API gateway) because the
         * unified-click-to-pay endpoint is served by the pay page proxy.
         */
        fun getUnifiedClickToPayUrl(): String {
            if (outletId != null && orderId != null && paymentRef != null) {
                val baseUrl = payPageUrl?.let { url ->
                    try {
                        val uri = android.net.Uri.parse(url)
                        "${uri.scheme}://${uri.host}"
                    } catch (e: Exception) {
                        null
                    }
                } ?: clickToPayUrl.substringBefore("/transactions/")
                    .ifEmpty { clickToPayUrl.substringBefore("/api/") }
                return "$baseUrl/api/outlets/$outletId/orders/$orderId/payments/$paymentRef/unified-click-to-pay"
            }
            // Fallback to the provided URL
            return clickToPayUrl
        }

        private fun toBundle() = bundleOf(EXTRA_ARGS to this)

        fun toIntent(context: Context) = Intent(
            context,
            ClickToPayActivity::class.java
        ).apply {
            putExtra(
                EXTRA_INTENT,
                toBundle()
            )
        }

        companion object {
            private const val EXTRA_ARGS = "click_to_pay_args"
            private const val EXTRA_INTENT = "click_to_pay_args_intent"

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
    fun onResult(result: ClickToPayLauncher.Result)
}

@Composable
fun rememberClickToPayLauncher(
    resultCallback: ResultCallback
): ClickToPayLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        ClickToPayLauncherContract(),
        resultCallback::onResult
    )
    return remember {
        ClickToPayLauncher(
            activityResultLauncher = activityResultLauncher,
        )
    }
}
