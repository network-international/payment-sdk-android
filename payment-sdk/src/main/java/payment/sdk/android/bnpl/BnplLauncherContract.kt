package payment.sdk.android.bnpl

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class BnplLauncherContract :
    ActivityResultContract<BnplLauncher.Config, BnplLauncher.Result>() {

    override fun createIntent(context: Context, input: BnplLauncher.Config): Intent =
        input.toIntent(context)

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): BnplLauncher.Result {
        val result: BnplLauncher.Result? =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_RESULT, BnplLauncher.Result::class.java)
            } else {
                intent?.getParcelableExtra(EXTRA_RESULT)
            }
        return result ?: BnplLauncher.Result.Failed("Error while processing the checkout result.")
    }

    internal companion object {
        internal const val EXTRA_RESULT = "bnpl_extra_result"
    }
}
