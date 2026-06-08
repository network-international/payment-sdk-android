package payment.sdk.android.payments.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderItem(
    val name: String,
    val amount: String
) : Parcelable
