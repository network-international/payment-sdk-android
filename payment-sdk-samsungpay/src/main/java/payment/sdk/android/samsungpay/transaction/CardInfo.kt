package payment.sdk.android.samsungpay.transaction

import android.os.Bundle
import payment.sdk.android.core.CardType

data class CardInfo(
        val cardType: CardType,
        val cardMetaData: Bundle?
)
