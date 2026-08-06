package payment.sdk.android.core

import androidx.annotation.Keep

/**
 * Response of `POST .../payments/{paymentRef}/benefit`.
 *
 * [paymentUrl] is a ready-to-load hosted page on the Benefit gateway. [status] is `Initiated` or
 * `Failed`.
 */
@Keep
data class BenefitInitResponse(
    val paymentId: String? = null,
    val paymentUrl: String? = null,
    val status: String? = null,
    val errorMessage: String? = null
) {
    val isInitiated: Boolean
        get() = status.equals(STATUS_INITIATED, ignoreCase = true)

    companion object {
        private const val STATUS_INITIATED = "Initiated"
    }
}
