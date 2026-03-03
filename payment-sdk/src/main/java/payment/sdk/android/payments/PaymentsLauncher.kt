package payment.sdk.android.payments

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * `UnifiedPaymentPageLauncher` class handles launching the payment page using an `ActivityResultLauncher`.
 * It supports asynchronous result handling through a callback.
 *
 * Usage:
 * ```
 * // Initialize the launcher
 * private val paymentsLauncher = UnifiedPaymentPageLauncher(
 *     this
 * ) { result ->
 *     // Handle the payment result of type [UnifiedPaymentPageResult]
 * }
 *
 * // Start the payment
 * paymentsLauncher.launch(
 *     UnifiedPaymentPageRequest.builder()
 *         .gatewayAuthorizationUrl(authUrl)
 *         .payPageUrl(payPageUrl)
 *         .setLanguageCode(viewModel.getLanguageCode())
 *         .build()
 * )
 * ```
 *
 * @property activityResultLauncher The launcher to start the payment page request.
 */
class UnifiedPaymentPageLauncher(private val activityResultLauncher: ActivityResultLauncher<UnifiedPaymentPageRequest>) {
    /**
     * Secondary constructor for `UnifiedPaymentPageLauncher`.
     *
     * @param activity The `ComponentActivity` from which the payment page is launched.
     * @param resultCallback The callback interface to handle the payment result.
     */
    constructor(
        activity: ComponentActivity,
        resultCallback: UnifiedPaymentPageResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            UnifiedPaymentPageLauncherContract(),
            resultCallback::onResult
        ),
    )

    /**
     * Launches the payment page using the provided `UnifiedPaymentPageRequest`.
     *
     * @param paymentsRequest The request object containing necessary parameters for launching the payment.
     */
    fun launch(paymentsRequest: UnifiedPaymentPageRequest) {
        activityResultLauncher.launch(paymentsRequest)
    }
}

/**
 * `UnifiedPaymentPageResultCallback` functional interface used to handle the result of the payment page request.
 */
fun interface UnifiedPaymentPageResultCallback {
    /**
     * Invoked with the `UnifiedPaymentPageResult` after the payment process completes.
     *
     * @param result The result of the payment, encapsulated in `UnifiedPaymentPageResult`.
     */
    fun onResult(result: UnifiedPaymentPageResult)
}


/**
 * A composable function to remember the `UnifiedPaymentPageLauncher` instance across recompositions.
 *
 * @param resultCallback The callback to handle the payment result.
 * @return An instance of `UnifiedPaymentPageLauncher`.
 */
@Composable
fun rememberUnifiedPaymentPageLauncher(
    resultCallback: UnifiedPaymentPageResultCallback
): UnifiedPaymentPageLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        UnifiedPaymentPageLauncherContract(),
        resultCallback::onResult
    )
    return remember { UnifiedPaymentPageLauncher(activityResultLauncher = activityResultLauncher) }
}