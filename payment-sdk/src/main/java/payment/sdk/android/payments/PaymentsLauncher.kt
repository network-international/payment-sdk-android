package payment.sdk.android.payments

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.parcelize.Parcelize

class PaymentsLauncher(private val activityResultLauncher: ActivityResultLauncher<PaymentsRequest>) {
    constructor(
        activity: ComponentActivity,
        resultCallback: PaymentsResultCallback,
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            PaymentsLauncherContract(),
            resultCallback::onResult
        ),
    )

    sealed class Result : Parcelable {
        @Parcelize
        data object Authorised : Result()

        @Parcelize
        data object Success : Result()

        @Parcelize
        data object PostAuthReview : Result()

        @Parcelize
        data object PartialAuthDeclined : Result()

        @Parcelize
        data object PartialAuthDeclineFailed : Result()

        @Parcelize
        data object PartiallyAuthorised : Result()

        @Parcelize
        data class Failed(val error: String) : Result()

        @Parcelize
        data object Cancelled : Result()
    }

    fun launch(paymentsRequest: PaymentsRequest) {
        activityResultLauncher.launch(paymentsRequest)
    }
}

fun interface PaymentsResultCallback {
    fun onResult(result: PaymentsLauncher.Result)
}

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