package payment.sdk.android.cardpayment.aaniPay.model

import payment.sdk.android.cardpayment.widget.LoadingMessage

sealed class AaniPayVMState {
    data object Init : AaniPayVMState()

    data class Loading(val message: LoadingMessage) : AaniPayVMState()

    data class Authorized(val accessToken: String) : AaniPayVMState()

    data class Pooling(val amount: Double, val currencyCode: String) : AaniPayVMState()

    data object Success : AaniPayVMState()

    data class Error(val message: String) : AaniPayVMState()
}