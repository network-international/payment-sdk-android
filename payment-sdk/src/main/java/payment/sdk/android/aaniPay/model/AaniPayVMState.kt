package payment.sdk.android.aaniPay.model

import payment.sdk.android.cardpayment.widget.LoadingMessage

internal sealed class AaniPayVMState {
    data object Init : AaniPayVMState()

    data class Loading(val message: LoadingMessage) : AaniPayVMState()

    data class Pooling(val amount: Double, val currencyCode: String, val deepLink: String) : AaniPayVMState()

    data object QrLoading : AaniPayVMState()

    data class QrDisplay(val amount: Double, val currencyCode: String, val qrContent: String) : AaniPayVMState()

    data class QrExpired(val amount: Double, val currencyCode: String) : AaniPayVMState()

    data class QrFailed(val amount: Double, val currencyCode: String) : AaniPayVMState()

    data class PaymentTimeout(val amount: Double, val currencyCode: String) : AaniPayVMState()

    data object Success : AaniPayVMState()

    data object Cancelled : AaniPayVMState()

    data class Error(val message: String) : AaniPayVMState()
}