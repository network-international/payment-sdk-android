package payment.sdk.android.cardpayment.card

import androidx.annotation.Keep
import payment.sdk.android.cardpayment.validation.Luhn
import payment.sdk.android.cardpayment.widget.ExpireDateEditText

@Keep
object CardValidator {
    fun isValid(
        paymentCard: PaymentCard?,
        pan: String,
        expiry: String,
        cvv: String,
        cardholderName: String
    ): Boolean = paymentCard?.let { card ->
        pan.takeIf { Luhn.isValidPan(it) }
            ?.takeIf { expiry.length >= 5 && ExpireDateEditText.isValidExpire(expiry) }
            ?.takeIf { card.cvv.length == cvv.length }
            ?.takeIf { cardholderName.isNotBlank() } != null
    } ?: false
}