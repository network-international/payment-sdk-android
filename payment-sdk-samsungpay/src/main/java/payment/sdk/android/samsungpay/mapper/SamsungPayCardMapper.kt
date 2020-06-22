package payment.sdk.android.samsungpay.mapper

import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import payment.sdk.android.core.CardType

class SamsungPayCardMapper {

    companion object {
        fun stringToSamsungPaySdk(cardType: String) =
                when (cardType) {
                    "VISA" -> SpaySdk.Brand.VISA
                    "MASTERCARD" -> SpaySdk.Brand.MASTERCARD
                    "AMERICAN_EXPRESS" -> SpaySdk.Brand.AMERICANEXPRESS
                    "DISCOVER" -> SpaySdk.Brand.DISCOVER
                    else -> null
                }
    }
}