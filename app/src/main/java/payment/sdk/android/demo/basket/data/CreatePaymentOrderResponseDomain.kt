package payment.sdk.android.demo.basket.data

import payment.sdk.android.core.CardType

data class CreatePaymentOrderResponseDomain(
        val orderReference: String,
        val paymentAuthorizationUrl: String,
        val code: String,
        val supportedCards: Set<CardType>
)
