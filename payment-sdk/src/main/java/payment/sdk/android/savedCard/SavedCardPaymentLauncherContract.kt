package payment.sdk.android.savedCard

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import payment.sdk.android.payments.UnifiedPaymentPageResult

internal class SavedCardPaymentLauncherContract :
    ActivityResultContract<SavedCardPaymentRequest, UnifiedPaymentPageResult>() {
    override fun createIntent(context: Context, input: SavedCardPaymentRequest): Intent {
        return input.toIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): UnifiedPaymentPageResult {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: UnifiedPaymentPageResult.Failed(
            "Error while processing result."
        )
    }

    internal companion object {
        internal const val EXTRA_RESULT = "saved_card_payments_extra_result"
    }
}