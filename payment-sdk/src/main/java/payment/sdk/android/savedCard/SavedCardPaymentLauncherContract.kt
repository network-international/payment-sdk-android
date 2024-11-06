package payment.sdk.android.savedCard

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import payment.sdk.android.payments.PaymentsResult

internal class SavedCardPaymentLauncherContract :
    ActivityResultContract<SavedCardPaymentRequest, PaymentsResult>() {
    override fun createIntent(context: Context, input: SavedCardPaymentRequest): Intent {
        return input.toIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentsResult {
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: PaymentsResult.Failed(
            "Error while processing result."
        )
    }

    internal companion object {
        internal const val EXTRA_RESULT = "saved_card_payments_extra_result"
    }
}