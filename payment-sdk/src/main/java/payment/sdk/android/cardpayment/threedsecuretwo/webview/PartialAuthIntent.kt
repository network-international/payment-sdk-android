package payment.sdk.android.cardpayment.threedsecuretwo.webview

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import payment.sdk.android.core.Order
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.ThreeDSAuthResponse

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

internal fun Order.toIntent(paymentCookie: String?): PartialAuthIntent? {
    return embedded?.payment?.firstOrNull()?.let { payment ->
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

internal fun ThreeDSAuthResponse.toIntent(paymentCookie: String?): PartialAuthIntent {
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

internal fun PaymentResponse.toIntent(paymentCookie: String?): PartialAuthIntent {
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
