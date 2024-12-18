package payment.sdk.android.savedCard

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import payment.sdk.android.payments.PaymentsResultCallback

/**
 * `SavedCardPaymentLauncher` is a utility class to launch a saved card payment
 *
 * Usage:
 * ```
 * private val savedCardPaymentLauncher = SavedCardPaymentLauncher(this) { result ->
 *     // Handle the payment result of type PaymentsResult
 * }
 *
 * savedCardPaymentLauncher.launch(
 *     SavedCardPaymentRequest.builder()
 *         .gatewayAuthorizationUrl(authUrl)
 *         .payPageUrl(payPageUrl)
 *         .setCvv(cvv)
 *         .build()
 * )
 * ```
 *
 * @property activityResultLauncher The launcher to handle the result of the saved card payment.
 * @constructor Creates a new `SavedCardPaymentLauncher` using the provided `ActivityResultLauncher`.
 * @param activity The `ComponentActivity` that launches the payment.
 * @param resultCallback A callback to handle the result of the payment.
 * @see SavedCardPaymentRequest
 */
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

    /**
     * Launches the saved card payment flow with the provided request parameters.
     *
     * @param savedCardPaymentRequest The request parameters for the saved card payment.
     */
    fun launch(savedCardPaymentRequest: SavedCardPaymentRequest) {
        activityResultLauncher.launch(savedCardPaymentRequest)
    }
}

/**
 * composable-friendly way, handling activity results asynchronously.
 *
 * @param resultCallback The callback to handle the payment result.
 * @return An instance of [SavedCardPaymentLauncher].
 */
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