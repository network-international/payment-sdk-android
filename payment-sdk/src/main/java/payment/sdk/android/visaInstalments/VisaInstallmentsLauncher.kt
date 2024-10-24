package payment.sdk.android.visaInstalments

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.payments.PaymentsRequest.Builder

class VisaInstallmentsLauncher(private val activityResultLauncher: ActivityResultLauncher<Config>) {
    constructor(
        activity: ComponentActivity,
        resultCallback: VisaInstallmentsResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            VisaInstallmentsLauncherContract(),
            resultCallback::onResult
        ),
    )

    @Parcelize
    data class Config(
        val installmentPlans: List<InstallmentPlan>,
        val cardNumber: String
    ) : Parcelable {
        fun toIntent(context: Context) = Intent(
            context,
            VisaInstallmentsActivity::class.java
        ).apply {
            putExtra(
                EXTRA_INTENT,
                toBundle()
            )
        }

        private fun toBundle() = bundleOf(EXTRA_ARGS to this)

        companion object {
            fun builder() = Builder()
            private const val EXTRA_ARGS = "vis_request_args"
            private const val EXTRA_INTENT = "vis_args_intent"

            fun fromIntent(intent: Intent): Config? {
                val inputIntent = intent.getBundleExtra(EXTRA_INTENT)
                return inputIntent?.getParcelable(EXTRA_ARGS)
            }
        }
    }

    sealed class Result : Parcelable {

        @Parcelize
        data class Success(val installmentPlan: InstallmentPlan) : Result()

        @Parcelize
        data object Cancelled : Result()
    }

    fun launch(config: Config) {
        activityResultLauncher.launch(config)
    }
}

fun interface VisaInstallmentsResultCallback {
    fun onResult(result: VisaInstallmentsLauncher.Result)
}