package payment.sdk.android.visaInstalments

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class VisaInstallmentsLauncherContract :
    ActivityResultContract<VisaInstallmentsLauncher.Config, VisaInstallmentsLauncher.Result>() {
    override fun createIntent(context: Context, input: VisaInstallmentsLauncher.Config): Intent {
        return input.toIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): VisaInstallmentsLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: VisaInstallmentsLauncher.Result.Cancelled
    }

    internal companion object {
        internal const val EXTRA_RESULT = "visa_installments_extra_result"
    }
}