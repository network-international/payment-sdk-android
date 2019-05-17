package payment.sdk.android.samsungpay.mapper

import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import payment.sdk.android.core.CardType

class SamsungPayCardMapper {

    companion object {

        fun mapNativeToSdk(samsungCardType: SpaySdk.Brand): CardType? =
                when (samsungCardType) {
                    SpaySdk.Brand.VISA -> CardType.Visa
                    SpaySdk.Brand.MASTERCARD -> CardType.MasterCard
                    SpaySdk.Brand.AMERICANEXPRESS -> CardType.AmericanExpress
                    SpaySdk.Brand.DISCOVER -> CardType.Discover
                    else -> null
                }


        fun mapSdkToNative(cardType: CardType) =
                when (cardType) {
                    CardType.Visa -> SpaySdk.Brand.VISA
                    CardType.MasterCard -> SpaySdk.Brand.MASTERCARD
                    CardType.AmericanExpress -> SpaySdk.Brand.AMERICANEXPRESS
                    CardType.Discover -> SpaySdk.Brand.DISCOVER
                    else -> null
                }
    }
}
