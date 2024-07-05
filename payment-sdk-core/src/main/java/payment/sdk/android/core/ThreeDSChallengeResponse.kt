package payment.sdk.android.core

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import payment.sdk.android.core.Order.Href

@Keep
data class ThreeDSChallengeResponse(
    val _embedded: Embedded
)

@Keep
class Embedded {
    lateinit var payment: Array<Payment>
}

@Keep
class Payment {
    @SerializedName(value = "_links")
    var links: PaymentLinks? = null
    var paymentMethod: PaymentMethod? = null
    var state: String? = null
    var amount: Order.Amount? = null
    var authResponse: AuthResponse? = null
}

@Keep
data class PaymentMethod(
    val issuingOrg: String? = null
)

@Keep
data class AuthResponse(
    val amount: Double? = null,
    val partialAmount: Double? = null
)

@Keep
class PaymentLinks {
    @SerializedName(value = "payment:partial-auth-accept")
    var partialAuthAccept: Href? = null

    @SerializedName(value = "payment:partial-auth-decline")
    var partialAuthDecline: Href? = null
}