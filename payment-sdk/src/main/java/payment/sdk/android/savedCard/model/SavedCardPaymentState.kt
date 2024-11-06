package payment.sdk.android.savedCard.model

import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.interactor.SavedCardPaymentApiRequest

internal sealed class SavedCardPaymentState {
    data object Init : SavedCardPaymentState()

    data class Loading(val message: LoadingMessage) : SavedCardPaymentState()

    data class CaptureCvv(
        val paymentCookie: String,
        val orderUrl: String,
        val orderAmount: OrderAmount,
        val savedCardPaymentRequest: SavedCardPaymentApiRequest,
        val visaPlans: VisaPlans? = null
    ) : SavedCardPaymentState()

    data class ShowVisaPlans(
        val savedCardPaymentRequest: SavedCardPaymentApiRequest,
        val visaPlans: VisaPlans,
        val orderUrl: String,
        val cardNumber: String,
        val orderAmount: OrderAmount,
        val paymentCookie: String,
    ) : SavedCardPaymentState()

    data class InitiatePartialAuth(val partialAuthIntent: PartialAuthIntent) :
        SavedCardPaymentState()
}