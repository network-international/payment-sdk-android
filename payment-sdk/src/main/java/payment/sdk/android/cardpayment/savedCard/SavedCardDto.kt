package payment.sdk.android.cardpayment.savedCard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import payment.sdk.android.core.SavedCard

@Parcelize
data class SavedCardDto(
    val cardholderName: String,
    val expiry: String,
    val maskedPan: String,
    val scheme: String,
    val cardToken: String,
    val recaptureCsc: Boolean
) : Parcelable {

    fun toSavedCard() = SavedCard(
        cardToken = cardToken,
        scheme = scheme,
        recaptureCsc = recaptureCsc,
        maskedPan = maskedPan,
        expiry = expiry,
        cardholderName = cardholderName
    )

    fun getExpiryFormatted(): String {
        val (year, month) = expiry.split("-")
        return "$month/${year.takeLast(2)}"
    }

    fun isAmex(): Boolean = scheme == "AMERICAN_EXPRESS"

    companion object {
        fun from(savedCard: SavedCard) = SavedCardDto(
            savedCard.cardholderName,
            savedCard.expiry,
            savedCard.maskedPan,
            savedCard.scheme,
            savedCard.cardToken,
            savedCard.recaptureCsc
        )
    }
}