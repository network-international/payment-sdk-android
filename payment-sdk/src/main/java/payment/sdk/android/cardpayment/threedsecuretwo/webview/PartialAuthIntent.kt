package payment.sdk.android.cardpayment.threedsecuretwo.webview

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.ThreeDSAuthResponse
import payment.sdk.android.core.ThreeDSChallengeResponse

@Parcelize
data class PartialAuthIntent(
    val paymentCookie: String?,
    val acceptUrl: String?,
    val declineUrl: String?,
    val currency: String?,
    val amount: Double?,
    val partialAmount: Double?,
    val issuingOrg: String?
) : Parcelable

fun ThreeDSChallengeResponse.toIntent(paymentCookie: String?): PartialAuthIntent? {
    return _embedded.payment.firstOrNull()?.let { payment ->
        PartialAuthIntent(
            paymentCookie = paymentCookie,
            acceptUrl = payment.links?.partialAuthAccept?.href,
            declineUrl = payment.links?.partialAuthDecline?.href,
            currency = payment.amount?.currencyCode,
            amount = payment.authResponse?.amount,
            partialAmount = payment.authResponse?.partialAmount,
            issuingOrg = payment.paymentMethod?.issuingOrg
        )
    }
}

fun ThreeDSAuthResponse.toIntent(paymentCookie: String?): PartialAuthIntent {
    return PartialAuthIntent(
        paymentCookie = paymentCookie,
        acceptUrl = links?.partialAuthAccept?.href,
        declineUrl = links?.partialAuthDecline?.href,
        currency = amount?.currencyCode,
        amount = authResponse?.amount,
        partialAmount = authResponse?.partialAmount,
        issuingOrg = paymentMethod?.issuingOrg
    )
}

fun PaymentResponse.toIntent(paymentCookie: String?): PartialAuthIntent {
    return PartialAuthIntent(
        paymentCookie = paymentCookie,
        acceptUrl = links?.partialAuthAccept?.href,
        declineUrl = links?.partialAuthDecline?.href,
        currency = amount?.currencyCode,
        amount = authResponse?.amount,
        partialAmount = authResponse?.partialAmount,
        issuingOrg = paymentMethod?.issuingOrg
    )
}
