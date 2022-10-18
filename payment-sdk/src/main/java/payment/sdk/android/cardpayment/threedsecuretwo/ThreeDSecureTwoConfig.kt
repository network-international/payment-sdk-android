package payment.sdk.android.cardpayment.threedsecuretwo

import payment.sdk.android.core.PaymentResponse

data class ThreeDSecureTwoConfig(
    val directoryServerID: String?,
    val threeDSMessageVersion: String?,
    val threeDSServerTransID: String?,
    val transStatus: String?,
    val threeDSMethodURL: String?,
    val acsTransID: String?,
    val acsReferenceNumber: String?,
    val acsSignedContent: String?,
    val authenticationCode: String?,
    val orderReference: String?,
    val outletId: String?,
    val threeDSTwoAuthenticationURL: String?,
    val threeDSTwoChallengeResponseURL: String?
) {
    companion object{
        fun buildFromPaymentResponse(paymentResponse: PaymentResponse): ThreeDSecureTwoConfig {
            val directoryServerID = paymentResponse.threeDSTwo?.directoryServerID
            val threeDSMessageVersion = paymentResponse.threeDSTwo?.messageVersion
            val threeDSServerTransID = paymentResponse.threeDSTwo?.threeDSServerTransID
            val transStatus = paymentResponse.threeDSTwo?.transStatus
            val threeDSMethodURL = paymentResponse.threeDSTwo?.threeDSMethodURL
            val acsTransID = paymentResponse.threeDSTwo?.acsTransID
            val acsReferenceNumber = paymentResponse.threeDSTwo?.acsReferenceNumber
            val acsSignedContent = paymentResponse.threeDSTwo?.acsSignedContent
            val authenticationCode = paymentResponse.authenticationCode
            val orderReference = paymentResponse.orderReference
            val outletId = paymentResponse.outletId
            val threeDSTwoAuthenticationURL = paymentResponse.links?.threeDSAuthenticationsUrl?.href
            val threeDSTwoChallengeResponseURL = paymentResponse.links?.threeDSChallengeResponseUrl?.href

            return ThreeDSecureTwoConfig(
                directoryServerID,
                threeDSMessageVersion,
                threeDSServerTransID,
                transStatus,
                threeDSMethodURL,
                acsTransID,
                acsReferenceNumber,
                acsSignedContent,
                authenticationCode,
                orderReference,
                outletId,
                threeDSTwoAuthenticationURL,
                threeDSTwoChallengeResponseURL
            )
        }
    }
}
