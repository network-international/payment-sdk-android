package payment.sdk.android.cardpayment.threedsecuretwo

data class ThreeDSTwoAuthenticationsResponse(
    val threeDSTwo: ThreeDSTwoConfig,
    val state: String
) {
}