package payment.sdk.android.qpay

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class QPayLauncherContract :
    ActivityResultContract<QPayLauncher.Config, QPayLauncher.Result>() {

    override fun createIntent(context: Context, input: QPayLauncher.Config): Intent =
        input.toIntent(context)

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): QPayLauncher.Result {
        val result: QPayLauncher.Result? =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_RESULT, QPayLauncher.Result::class.java)
            } else {
                intent?.getParcelableExtra(EXTRA_RESULT)
            }
        return result ?: QPayLauncher.Result.Failed("Error while processing result from QPay.")
    }

    internal companion object {
        internal const val EXTRA_RESULT = "qpay_extra_result"
    }
}
