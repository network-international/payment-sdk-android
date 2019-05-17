package payment.sdk.android.cardpayment

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import java.lang.IllegalStateException

class CardPaymentData constructor(
        val code: Int,
        val reason: String? = null
) : Parcelable {

    internal constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString())

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(code)
        dest?.writeString(reason)
    }

    override fun describeContents(): Int = 0

    companion object IntentResolver {
        const val STATUS_GENERIC_ERROR: Int = -1
        const val STATUS_PAYMENT_FAILED: Int = 0
        const val STATUS_PAYMENT_AUTHORIZED: Int = 1
        const val STATUS_PAYMENT_CAPTURED: Int = 2

        internal const val INTENT_DATA_KEY = "data"

        @JvmStatic
        fun getFromIntent(intent: Intent): CardPaymentData =
                intent.getParcelableExtra(INTENT_DATA_KEY)
                        ?: throw IllegalStateException("Payment result not found in intent")

        @JvmField
        val CREATOR: Parcelable.Creator<CardPaymentData> = object : Parcelable.Creator<CardPaymentData> {
            override fun createFromParcel(parcel: Parcel): CardPaymentData {
                return CardPaymentData(parcel)
            }

            override fun newArray(size: Int): Array<CardPaymentData?> {
                return arrayOfNulls(size)
            }
        }
    }
}