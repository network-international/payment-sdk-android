package payment.sdk.android.core

import com.google.gson.annotations.SerializedName

data class ThreeDSAuthResponse(
    @SerializedName(value = "_links")
    val links: PaymentLinks? = null,
    var paymentMethod: PaymentMethod? = null,
    val authResponse: AuthResponse? = null,
    val state: String? = null,
    val amount: Order.Amount? = null
)
