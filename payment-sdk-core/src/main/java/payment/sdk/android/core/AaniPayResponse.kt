package payment.sdk.android.core

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AaniPayResponse(
    val _id: String? = null,
    val _links: Links? = null,
    val aani: Aani?,
    val amount: Order.Amount,
    val orderReference: String? = null,
    val outletId: String? = null,
    val reference: String? = null,
    val state: String
)

@Keep
data class Links(
    @SerializedName("cnp:aani-status") val aaniStatus: Order.Href,
    @SerializedName("self") val self: Order.Href
)

@Keep
data class Aani(
    val deepLinkUrl: String?,
    val qrCodeId: String? = null,
    val qrCodeTransactionId: String? = null
)