package payment.sdk.android.aaniPay

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

class AaniPayLauncher(
    private val activityResultLauncher: ActivityResultLauncher<Config>
) {
    constructor(
        activity: ComponentActivity,
        resultCallback: ResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            AaniPayLauncherContract(),
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
        data class Failed(
            val error: String
        ) : Result()

        @Parcelize
        data object Canceled : Result()
    }

    @Parcelize
    class Config(
        val amount: Double,
        val anniPaymentLink: String,
        val currencyCode: String,
        val accessToken: String,
        val payerIp: String,
    ) : Parcelable {

        private fun toBundle() = bundleOf(EXTRA_ARGS to this)

        fun toIntent(context: Context) = Intent(
            context,
            AaniPayActivity::class.java
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
        }
    }
}

fun interface ResultCallback {
    fun onResult(result: AaniPayLauncher.Result)
}

@Composable
fun rememberAaniPayLauncher(
    resultCallback: ResultCallback
): AaniPayLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        AaniPayLauncherContract(),
        resultCallback::onResult
    )
    return remember {
        AaniPayLauncher(
            activityResultLauncher = activityResultLauncher,
        )
    }
}