package payment.sdk.android.benefit

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class BenefitLauncherContract :
    ActivityResultContract<BenefitLauncher.Config, BenefitLauncher.Result>() {

    override fun createIntent(context: Context, input: BenefitLauncher.Config): Intent =
        input.toIntent(context)

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): BenefitLauncher.Result {
        val result: BenefitLauncher.Result? =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_RESULT, BenefitLauncher.Result::class.java)
            } else {
                intent?.getParcelableExtra(EXTRA_RESULT)
            }
        return result ?: BenefitLauncher.Result.Failed("Error while processing result from Benefit.")
    }

    internal companion object {
        internal const val EXTRA_RESULT = "benefit_extra_result"
    }
}
