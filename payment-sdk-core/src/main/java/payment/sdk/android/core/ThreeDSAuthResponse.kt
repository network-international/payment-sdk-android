package payment.sdk.android.core

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ThreeDSAuthResponse(
    @SerializedName(value = "_links")
    val links: Order.PaymentLinks? = null,
    var paymentMethod: PaymentMethod? = null,
    val authResponse: AuthResponse? = null,
    val state: String? = null,
    val amount: Order.Amount? = null
)

@Keep
data class PaymentMethod(
    val issuingOrg: String? = null
)

@Keep
data class AuthResponse(
    val amount: Double? = null,
    val partialAmount: Double? = null
)