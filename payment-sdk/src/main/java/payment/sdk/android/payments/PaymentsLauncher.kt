package payment.sdk.android.payments

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * `PaymentsLauncher` class handles launching the payment page using an `ActivityResultLauncher`.
 * It supports asynchronous result handling through a callback.
 *
 * Usage:
 * ```
 * // Initialize the launcher
 * private val paymentsLauncher = PaymentsLauncher(
 *     this
 * ) { result ->
 *     // Handle the payment result of type [PaymentsResult]
 * }
 *
 * // Start the payment
 * paymentsLauncher.launch(
 *     PaymentsRequest.builder()
 *         .gatewayAuthorizationUrl(authUrl)
 *         .payPageUrl(payPageUrl)
 *         .setLanguageCode(viewModel.getLanguageCode())
 *         .build()
 * )
 * ```
 *
 * @property activityResultLauncher The launcher to start the payment page request.
 */
class PaymentsLauncher(private val activityResultLauncher: ActivityResultLauncher<PaymentsRequest>) {
    /**
     * Secondary constructor for `PaymentsLauncher`.
     *
     * @param activity The `ComponentActivity` from which the payment page is launched.
     * @param resultCallback The callback interface to handle the payment result.
     */
    constructor(
        activity: ComponentActivity,
        resultCallback: PaymentsResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            PaymentsLauncherContract(),
            resultCallback::onResult
        ),
    )

    /**
     * Launches the payment page using the provided `PaymentsRequest`.
     *
     * @param paymentsRequest The request object containing necessary parameters for launching the payment.
     */
    fun launch(paymentsRequest: PaymentsRequest) {
        activityResultLauncher.launch(paymentsRequest)
    }
}

/**
 * `PaymentsResultCallback` functional interface used to handle the result of the payment page request.
 */
fun interface PaymentsResultCallback {
    /**
     * Invoked with the `PaymentsResult` after the payment process completes.
     *
     * @param result The result of the payment, encapsulated in `PaymentsResult`.
     */
    fun onResult(result: PaymentsResult)
}


/**
 * A composable function to remember the `PaymentsLauncher` instance across recompositions.
 *
 * @param resultCallback The callback to handle the payment result.
 * @return An instance of `PaymentsLauncher`.
 */
@Composable
fun rememberPaymentsLauncher(
    resultCallback: PaymentsResultCallback
): PaymentsLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        PaymentsLauncherContract(),
        resultCallback::onResult
    )
    return remember { PaymentsLauncher(activityResultLauncher = activityResultLauncher) }
}