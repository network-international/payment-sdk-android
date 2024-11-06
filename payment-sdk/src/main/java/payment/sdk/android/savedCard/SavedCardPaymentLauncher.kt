package payment.sdk.android.savedCard

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import payment.sdk.android.payments.PaymentsResultCallback

class SavedCardPaymentLauncher(private val activityResultLauncher: ActivityResultLauncher<SavedCardPaymentRequest>) {

    constructor(
        activity: ComponentActivity,
        resultCallback: PaymentsResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            SavedCardPaymentLauncherContract(),
            resultCallback::onResult
        ),
    )

    fun launch(savedCardPaymentRequest: SavedCardPaymentRequest) {
        activityResultLauncher.launch(savedCardPaymentRequest)
    }
}

@Composable
fun rememberSavedCardPaymentLauncher(
    resultCallback: PaymentsResultCallback
): SavedCardPaymentLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        SavedCardPaymentLauncherContract(),
        resultCallback::onResult
    )
    return remember { SavedCardPaymentLauncher(activityResultLauncher = activityResultLauncher) }
}