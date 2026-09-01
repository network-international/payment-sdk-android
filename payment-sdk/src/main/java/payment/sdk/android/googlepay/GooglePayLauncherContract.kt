package payment.sdk.android.googlepay

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class GooglePayLauncherContract :
    ActivityResultContract<GooglePayLauncher.Config, GooglePayLauncher.Result>() {

    override fun createIntent(context: Context, input: GooglePayLauncher.Config): Intent =
        input.toIntent(context)

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): GooglePayLauncher.Result {
        val result: GooglePayLauncher.Result? =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_RESULT, GooglePayLauncher.Result::class.java)
            } else {
                intent?.getParcelableExtra(EXTRA_RESULT)
            }
        return result ?: GooglePayLauncher.Result.Failed("Error while processing result from Google Pay.")
    }

    internal companion object {
        internal const val EXTRA_RESULT = "google_pay_extra_result"
    }
}
