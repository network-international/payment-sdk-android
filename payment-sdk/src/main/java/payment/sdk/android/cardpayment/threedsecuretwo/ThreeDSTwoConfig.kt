package payment.sdk.android.cardpayment.threedsecuretwo

data class ThreeDSTwoConfig(
    val directoryServerID: String,
    val threeDSServerTransID: String,
    val messageVersion: String,
    val transStatus: String,
    val threeDSMethodURL: String,
    val acsTransID: String,
    val acsReferenceNumber: String,
    val acsSignedContent: String
) {

}