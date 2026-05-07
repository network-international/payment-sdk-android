package payment.sdk.android.core

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SliceAmount(
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("value") val value: Int
)

@Keep
data class SliceOffer(
    @SerializedName("period") val period: String,
    @SerializedName("rate") val rate: String,
    @SerializedName("fee") val fee: String,
    @SerializedName("feeType") val feeType: String,
    @SerializedName("installmentAmount") val installmentAmount: SliceAmount,
    @SerializedName("totalAmount") val totalAmount: SliceAmount
)

@Keep
data class SliceEligibilityResponse(
    @SerializedName("transactionAmount") val transactionAmount: SliceAmount,
    @SerializedName("offers") val offers: List<SliceOffer>
)
