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
import payment.sdk.android.core.Order

class GooglePayLauncher(
    private val activityResultLauncher: ActivityResultLauncher<Config>,
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: GooglePayResultCallback,
    ): this(
        activityResultLauncher = activity.registerForActivityResult(
            GooglePayLauncherContract(),
            resultCallback::onResult
        )
    )

    sealed class Result : Parcelable {
        @Parcelize
        data object Success : Result()

        @Parcelize
        data class Failed(
            val error: String
        ) : Result()

        @Parcelize
        data object Canceled : Result()
    }

    fun launch(config: Config) {
        activityResultLauncher.launch(config)
    }

    @Parcelize
    class Config(
        val amount: Double,
        val googlePayLink: String,
        val currencyCode: String,
        val authUrl: String,
        val payPageUrl: String,
    ): Parcelable {
        private fun toBundle() = bundleOf(EXTRA_ARGS to this)

        fun toIntent(context: Context) = Intent(
            context,
            GooglePayActivity::class.java
        ).apply {
            putExtra(
                EXTRA_INTENT,
                toBundle()
            )
        }

        companion object {
            private const val EXTRA_ARGS = "aani_pay_args"
            private const val EXTRA_INTENT = "aani_pay_args_intent"

            fun fromIntent(intent: Intent): Config? {
                val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
                return inputIntent?.getParcelable(EXTRA_ARGS)
            }

            @Throws(IllegalArgumentException::class)
            fun create(
                order: Order,
            ): Config {
                return Config(
                    amount = requireNotNull(order.amount?.value) {
                        "Order Amount Not found"
                    },
                    currencyCode = requireNotNull(order.amount?.currencyCode) {
                        "Currency Code not found"
                    },
                    googlePayLink = requireNotNull(order.embedded?.payment?.first()?.links?.googlePayLink?.href) {
                        "Aani Payment Link not found"
                    },
                    payPageUrl = requireNotNull(order.links?.paymentUrl?.href) { "Payment URL not found" },
                    authUrl = requireNotNull(order.links?.paymentAuthorizationUrl?.href) { "Auth URL not found " },
                )
            }
        }
    }
}

fun interface GooglePayResultCallback {
    fun onResult(result: GooglePayLauncher.Result)
}

@Composable
fun rememberGooglePayLauncher(
    resultCallback: GooglePayResultCallback
): GooglePayLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        GooglePayLauncherContract(),
        resultCallback::onResult
    )
    return remember {
        GooglePayLauncher(
            activityResultLauncher = activityResultLauncher,
        )
    }
}