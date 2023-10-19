package payment.sdk.android.cardpayment.threedsecuretwo

import payment.sdk.android.core.PaymentResponse
import kotlin.jvm.Throws

class ThreeDSecureFactory {

    @Throws(IllegalArgumentException::class)
    fun buildThreeDSecureTwoDto(
        paymentResponse: PaymentResponse,
        paymentCookie: String,
        orderUrl: String
    ): ThreeDSecureTwoDto {
        val threeDSecureRequest = ThreeDSecureTwoRequest.buildFromPaymentResponse(paymentResponse = paymentResponse)
        return ThreeDSecureTwoDto(
            paymentCookie = paymentCookie,
            orderUrl = orderUrl,
            directoryServerID = requireNotNull(paymentResponse.threeDSTwo?.directoryServerID) {
                "directoryServerID not found"
            },
            orderRef = requireNotNull(paymentResponse.orderReference) {
                "order ref not found"
            },
            paymentReference = requireNotNull(paymentResponse.reference) {
                "Payment reference not found"
            },
            outletRef = requireNotNull(paymentResponse.outletId) {
                "outlet id not found"
            },
            threeDSMessageVersion = requireNotNull(paymentResponse.threeDSTwo?.messageVersion) {
                "threeDSMessageVersion not found"
            },
            threeDSMethodData = threeDSecureRequest.threeDSMethodData,
            threeDSMethodNotificationURL = threeDSecureRequest.threeDSMethodNotificationURL,
            threeDSMethodURL = paymentResponse.threeDSTwo?.threeDSMethodURL,
            threeDSServerTransID = paymentResponse.threeDSTwo?.threeDSServerTransID,
            threeDSTwoAuthenticationURL = requireNotNull(
                paymentResponse.links?.threeDSAuthenticationsUrl?.href
            ) {
                "threeDSTwoAuthenticationURL not found"
            },
            threeDSTwoChallengeResponseURL = requireNotNull(
                paymentResponse.links?.threeDSChallengeResponseUrl?.href
            ) {
                "3ds challenge response url not found"
            }
        )
    }

    @Throws(IllegalArgumentException::class)
    fun buildThreeDSecureDto(
        paymentResponse: PaymentResponse
    ): ThreeDSecureDto {
        return ThreeDSecureDto(
            acsMd = requireNotNull(paymentResponse.threeDSOne?.acsMd) {
                "ThreeDS one acsMd not found"
            },
            acsPaReq = requireNotNull(paymentResponse.threeDSOne?.acsPaReq) {
                "ThreeDS one acsPaReq not found"
            },
            acsUrl = requireNotNull(paymentResponse.threeDSOne?.acsUrl) {
                "ThreeDS one acs url not found"
            },
            threeDSOneUrl = requireNotNull(paymentResponse.links?.threeDSOneUrl?.href) {
                "ThreeDS one url not found"
            }
        )
    }
}

data class ThreeDSecureTwoDto(
    val threeDSMethodData: String?,
    val threeDSMethodNotificationURL: String,
    val threeDSMethodURL: String?,
    val threeDSServerTransID: String?,
    val paymentCookie: String,
    val threeDSTwoAuthenticationURL: String,
    val directoryServerID: String,
    val threeDSMessageVersion: String,
    val threeDSTwoChallengeResponseURL: String,
    val outletRef: String,
    val orderRef: String,
    val orderUrl: String,
    val paymentReference: String
)

data class ThreeDSecureDto(
    val acsUrl: String,
    val acsPaReq: String,
    val acsMd: String,
    val threeDSOneUrl: String,
)