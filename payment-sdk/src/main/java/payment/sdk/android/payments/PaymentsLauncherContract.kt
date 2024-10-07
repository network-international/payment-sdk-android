package payment.sdk.android.payments

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class PaymentsLauncherContract :
    ActivityResultContract<PaymentsRequest, CardPaymentsLauncher.Result>() {
    override fun createIntent(context: Context, input: PaymentsRequest): Intent {
        return input.toIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CardPaymentsLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: CardPaymentsLauncher.Result.Failed(
            "Error while processing result."
        )
    }

    internal companion object {
        internal const val  EXTRA_RESULT = "card_payments_extra_result"
    }
}