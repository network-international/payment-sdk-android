package payment.sdk.android.clicktopay

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class ClickToPayLauncherContract :
    ActivityResultContract<ClickToPayLauncher.Config, ClickToPayLauncher.Result>() {

    override fun createIntent(context: Context, input: ClickToPayLauncher.Config): Intent {
        return input.toIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ClickToPayLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: ClickToPayLauncher.Result.Failed(
            "Error while processing result from Click to Pay."
        )
    }

    internal companion object {
        internal const val EXTRA_RESULT = "click_to_pay_extra_result"
    }
}
