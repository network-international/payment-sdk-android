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
    /**
     * Optional installment fee charged by the issuer, as a major-unit amount string
     * (e.g. "7.00"). Only rendered when present and greater than zero.
     */
    @SerializedName("commission") val commission: String? = null,
    @SerializedName("installmentAmount") val installmentAmount: SliceAmount,
    @SerializedName("totalAmount") val totalAmount: SliceAmount
) {
    /**
     * Parsed [commission] in major units, non-null only when the backend sent the flag
     * with a value greater than zero — the condition for showing the Installment Fee row.
     */
    val installmentFeeAmount: Double?
        get() = commission?.toDoubleOrNull()?.takeIf { it > 0 }
}

@Keep
data class SliceEligibilityResponse(
    @SerializedName("transactionAmount") val transactionAmount: SliceAmount,
    @SerializedName("offers") val offers: List<SliceOffer>,
    /**
     * Backend flag: "Y" = conventional interest-based offers, "I" = Islamic / Murabaha,
     * "N" = ineligible (no offers shown). Other values are treated as conventional.
     */
    @SerializedName("indicator") val indicator: String? = null,
)
