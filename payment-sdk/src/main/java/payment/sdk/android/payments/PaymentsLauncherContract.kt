package payment.sdk.android.payments

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class UnifiedPaymentPageLauncherContract :
    ActivityResultContract<UnifiedPaymentPageRequest, UnifiedPaymentPageResult>() {
    override fun createIntent(context: Context, input: UnifiedPaymentPageRequest): Intent {
        return input.toIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): UnifiedPaymentPageResult {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: UnifiedPaymentPageResult.Failed(
            "Error while processing result."
        )
    }

    internal companion object {
        internal const val EXTRA_RESULT = "card_payments_extra_result"
    }
}