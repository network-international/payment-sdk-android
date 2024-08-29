package payment.sdk.android.googlepay

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class GooglePayLauncherContract :
    ActivityResultContract<GooglePayLauncher.Config, GooglePayLauncher.Result>() {
    override fun createIntent(context: Context, input: GooglePayLauncher.Config): Intent {
        return input.toIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): GooglePayLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: GooglePayLauncher.Result.Failed(
            "Error while processing result from google Pay."
        )
    }

    internal companion object {
        internal const val EXTRA_RESULT = "google_pay_extra_result"
    }
}