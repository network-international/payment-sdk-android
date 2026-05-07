package payment.sdk.android.core

import androidx.annotation.Keep

@Keep
data class SliceRequest(
    val period: String,
    val rate: String,
    val fee: String
)
