package payment.sdk.android.knet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class KnetLauncherContract :
    ActivityResultContract<KnetLauncher.Config, KnetLauncher.Result>() {

    override fun createIntent(context: Context, input: KnetLauncher.Config): Intent =
        input.toIntent(context)

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): KnetLauncher.Result {
        val result: KnetLauncher.Result? =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_RESULT, KnetLauncher.Result::class.java)
            } else {
                intent?.getParcelableExtra(EXTRA_RESULT)
            }
        return result ?: KnetLauncher.Result.Failed("Error while processing result from KNET.")
    }

    internal companion object {
        internal const val EXTRA_RESULT = "knet_extra_result"
    }
}
