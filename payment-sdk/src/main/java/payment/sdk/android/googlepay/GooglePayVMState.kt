package payment.sdk.android.googlepay

import com.google.android.gms.wallet.PaymentDataRequest

sealed class GooglePayVMState {
    data object Init : GooglePayVMState()
    class Authorized(val accessToken: String, val paymentCookie: String, val orderUrl: String) :
        GooglePayVMState()

    class Error(val error: String) : GooglePayVMState()
    class Submit(val paymentDataRequest: PaymentDataRequest, val allowedPaymentMethods: String) :
        GooglePayVMState()
}