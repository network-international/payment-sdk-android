package payment.sdk.android.aaniPay

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class AaniPayLauncherContract :
    ActivityResultContract<AaniPayLauncher.Config, AaniPayLauncher.Result>() {

    override fun createIntent(context: Context, input: AaniPayLauncher.Config): Intent {
        return input.toIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): AaniPayLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: AaniPayLauncher.Result.Failed(
            "Error while processing result from Aani Pay."
        )
    }

    internal companion object {
        internal const val EXTRA_RESULT = "aani_extra_result"
    }
}