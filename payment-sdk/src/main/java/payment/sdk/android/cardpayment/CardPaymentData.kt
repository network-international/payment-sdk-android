package payment.sdk.android.cardpayment

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import payment.sdk.android.payments.PaymentsResult

class CardPaymentData constructor(
    val code: Int,
    val reason: String? = null
) : Parcelable {

    internal constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(code)
        dest.writeString(reason)
    }

    fun getCardPaymentsState(): PaymentsResult {
        return when (code) {
            STATUS_PAYMENT_AUTHORIZED -> PaymentsResult.Authorised
            STATUS_PAYMENT_PURCHASED, STATUS_PAYMENT_CAPTURED -> PaymentsResult.Success
            STATUS_POST_AUTH_REVIEW -> PaymentsResult.PostAuthReview
            STATUS_PARTIAL_AUTH_DECLINED -> PaymentsResult.PartialAuthDeclined
            STATUS_PARTIAL_AUTH_DECLINE_FAILED -> PaymentsResult.PartialAuthDeclineFailed
            STATUS_PARTIALLY_AUTHORISED -> PaymentsResult.PartiallyAuthorised
            STATUS_PAYMENT_FAILED -> PaymentsResult.Failed(reason.orEmpty())
            else -> throw IllegalArgumentException("Cannot Parse CardPayment Data Intent")
        }
    }

    override fun describeContents(): Int = 0

    companion object IntentResolver {
        const val STATUS_GENERIC_ERROR: Int = -1
        const val STATUS_PAYMENT_FAILED: Int = 0
        const val STATUS_PAYMENT_AUTHORIZED: Int = 1
        const val STATUS_PAYMENT_PURCHASED: Int = 3
        const val STATUS_PAYMENT_CAPTURED: Int = 2
        const val STATUS_POST_AUTH_REVIEW: Int = 4
        const val STATUS_PARTIAL_AUTH_DECLINED: Int = 5
        const val STATUS_PARTIAL_AUTH_DECLINE_FAILED: Int = 6
        const val STATUS_PARTIALLY_AUTHORISED: Int = 7

        internal const val INTENT_DATA_KEY = "data"

        @JvmStatic
        fun getFromIntent(intent: Intent): CardPaymentData =
            intent.getParcelableExtra(INTENT_DATA_KEY)
                ?: throw IllegalStateException("Payment result not found in intent")

        @JvmField
        val CREATOR: Parcelable.Creator<CardPaymentData> =
            object : Parcelable.Creator<CardPaymentData> {
                override fun createFromParcel(parcel: Parcel): CardPaymentData {
                    return CardPaymentData(parcel)
                }

                override fun newArray(size: Int): Array<CardPaymentData?> {
                    return arrayOfNulls(size)
                }
            }
    }
}