package payment.sdk.android.core

import androidx.annotation.Keep

@Keep
data class SavedCard(
    val cardholderName: String,
    val expiry: String,
    val maskedPan: String,
    val scheme: String,
    val cardToken: String,
    val recaptureCsc: Boolean
)
