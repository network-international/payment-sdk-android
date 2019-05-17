package payment.sdk.android.demo.basket.data

data class CreatePaymentOrderResponseDto(
        val orderReference: String,
        val paymentAuthorizationUrl: String,
        val code: String,
        val supportedCards: List<String>
)
