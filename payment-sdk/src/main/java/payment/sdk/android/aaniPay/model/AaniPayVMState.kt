package payment.sdk.android.aaniPay.model

import payment.sdk.android.cardpayment.widget.LoadingMessage

internal sealed class AaniPayVMState {
    data object Init : AaniPayVMState()

    data class Loading(val message: LoadingMessage) : AaniPayVMState()

    data class Pooling(val amount: Double, val currencyCode: String, val deepLink: String) : AaniPayVMState()

    data object Success : AaniPayVMState()

    data class Error(val message: String) : AaniPayVMState()
}