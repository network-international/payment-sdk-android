package payment.sdk.android.core

import com.google.gson.annotations.SerializedName

data class AaniPayResponse(
    val _id: String,
    val _links: Links,
    val aani: Aani?,
    val amount: Order.Amount,
    val orderReference: String,
    val outletId: String,
    val reference: String,
    val state: String
)

data class Links(
    @SerializedName("cnp:aani-status") val aaniStatus: Order.Href,
    @SerializedName("self") val self: Order.Href
)

data class Aani(
    val deepLinkUrl: String?
)