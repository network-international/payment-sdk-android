package payment.sdk.android.core

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class SavedCard(
    val cardholderName: String,
    val expiry: String,
    val maskedPan: String,
    val scheme: String,
    val cardToken: String,
    val recaptureCsc: Boolean
) : Parcelable
